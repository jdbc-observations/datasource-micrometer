/*
 * Copyright 2022-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ttddyy.observation.boot.autoconfigure;

import javax.sql.DataSource;

import net.ttddyy.dsproxy.listener.MethodExecutionListener;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.proxy.ProxyJdbcObject;
import net.ttddyy.dsproxy.proxy.ResultSetProxyLogicFactory;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import net.ttddyy.dsproxy.transform.ParameterTransformer;
import net.ttddyy.dsproxy.transform.QueryTransformer;
import net.ttddyy.observation.boot.autoconfigure.JdbcProperties.DataSourceType;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.util.ClassUtils;

/**
 * A {@link BeanPostProcessor} to instrument {@link DataSource} beans.
 *
 * @author Tadaya Tsuyukubo
 */
public class DataSourceObservationBeanPostProcessor implements BeanPostProcessor {

	static final boolean SPRING_JDBC_PRESENT = ClassUtils.isPresent(
			"org.springframework.jdbc.datasource.DelegatingDataSource",
			DataSourceObservationBeanPostProcessor.class.getClassLoader());

	private final ObjectProvider<JdbcProperties> jdbcPropertiesProvider;

	private final ObjectProvider<DataSourceNameResolver> dataSourceNameResolverProvider;

	private final ObjectProvider<QueryExecutionListener> listenersProvider;

	private final ObjectProvider<MethodExecutionListener> methodExecutionListenersProvider;

	private final ObjectProvider<ParameterTransformer> parameterTransformerProvider;

	private final ObjectProvider<QueryTransformer> queryTransformerProvider;

	private final ObjectProvider<ResultSetProxyLogicFactory> resultSetProxyLogicFactoryProvider;

	private final ObjectProvider<ResultSetProxyLogicFactory> generatedKeysProxyLogicFactoryProvider;

	private final ObjectProvider<DataSourceProxyConnectionIdManagerProvider> dataSourceProxyConnectionIdManagerProviderProvider;

	private DataSourceProxyBuilderConfigurer dataSourceProxyBuilderConfigurer;

	private final ObjectProvider<ProxyDataSourceBuilderCustomizer> proxyDataSourceBuilderCustomizers;

	public DataSourceObservationBeanPostProcessor(ObjectProvider<JdbcProperties> jdbcPropertiesProvider,
			ObjectProvider<DataSourceNameResolver> dataSourceNameResolverProvider,
			ObjectProvider<QueryExecutionListener> listenersProvider,
			ObjectProvider<MethodExecutionListener> methodExecutionListenersProvider,
			ObjectProvider<ParameterTransformer> parameterTransformerProvider,
			ObjectProvider<QueryTransformer> queryTransformerProvider,
			ObjectProvider<ResultSetProxyLogicFactory> resultSetProxyLogicFactoryProvider,
			ObjectProvider<ResultSetProxyLogicFactory> generatedKeysProxyLogicFactoryProvider,
			ObjectProvider<DataSourceProxyConnectionIdManagerProvider> dataSourceProxyConnectionIdManagerProviderProvider,
			ObjectProvider<ProxyDataSourceBuilderCustomizer> proxyDataSourceBuilderCustomizers) {
		this.jdbcPropertiesProvider = jdbcPropertiesProvider;
		this.dataSourceNameResolverProvider = dataSourceNameResolverProvider;
		this.listenersProvider = listenersProvider;
		this.methodExecutionListenersProvider = methodExecutionListenersProvider;
		this.parameterTransformerProvider = parameterTransformerProvider;
		this.queryTransformerProvider = queryTransformerProvider;
		this.resultSetProxyLogicFactoryProvider = resultSetProxyLogicFactoryProvider;
		this.generatedKeysProxyLogicFactoryProvider = generatedKeysProxyLogicFactoryProvider;
		this.dataSourceProxyConnectionIdManagerProviderProvider = dataSourceProxyConnectionIdManagerProviderProvider;
		this.proxyDataSourceBuilderCustomizers = proxyDataSourceBuilderCustomizers;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof DataSource dataSource && !ScopedProxyUtils.isScopedTarget(beanName)
				&& !isExcludedBean(beanName) && !containsAlreadyProxiedTarget(dataSource)) {
			String dataSourceName = this.dataSourceNameResolverProvider.getObject().resolve(beanName, dataSource);
			ProxyDataSourceBuilder builder = ProxyDataSourceBuilder.create(dataSourceName, dataSource);
			getConfigurer().configure(builder);
			this.proxyDataSourceBuilderCustomizers.orderedStream()
				.forEach(customizer -> customizer.customize(builder, dataSource, beanName, dataSourceName));
			DataSourceType dataSourceType = getJdbcProperties().getDatasourceProxy().getType();
			if (dataSourceType == DataSourceType.PROXY || dataSourceType == DataSourceType.SPRING_PROXY) {
				return builder.buildProxy();
			}
			else {
				return builder.build();
			}
		}
		else {
			return bean;
		}
	}

	/**
	 * Returns {@code true} if {@code ds} is a routing or delegating wrapper whose chain
	 * contains a datasource already instrumented by datasource-proxy.
	 *
	 * <p>When {@code spring-jdbc} is absent the check is skipped and returns {@code false},
	 * preserving the pre-existing behaviour of wrapping all datasource beans.
	 */
	private static boolean containsAlreadyProxiedTarget(DataSource ds) {
		if (SPRING_JDBC_PRESENT) {
			return SpringJdbcDelegate.containsProxiedTarget(ds);
		}
		return false;
	}

	private DataSourceProxyBuilderConfigurer getConfigurer() {
		if (this.dataSourceProxyBuilderConfigurer == null) {
			this.dataSourceProxyBuilderConfigurer = new DataSourceProxyBuilderConfigurer(getJdbcProperties(),
					this.listenersProvider.orderedStream().toList(),
					this.methodExecutionListenersProvider.orderedStream().toList(),
					this.parameterTransformerProvider.getIfAvailable(), this.queryTransformerProvider.getIfAvailable(),
					this.resultSetProxyLogicFactoryProvider.getIfAvailable(),
					this.generatedKeysProxyLogicFactoryProvider.getIfAvailable(),
					this.dataSourceProxyConnectionIdManagerProviderProvider.getIfAvailable());
		}
		return this.dataSourceProxyBuilderConfigurer;
	}

	private boolean isExcludedBean(String beanName) {
		return getJdbcProperties().getExcludedDataSourceBeanNames().contains(beanName);
	}

	private JdbcProperties getJdbcProperties() {
		return this.jdbcPropertiesProvider.getObject();
	}

	/**
	 * Isolated in a separate class so that the JVM only loads it when
	 * {@code spring-jdbc} is actually on the classpath. If it were inlined in the
	 * outer class, the mere presence of {@link DelegatingDataSource} and
	 * {@link AbstractRoutingDataSource} in the constant pool would cause a
	 * {@link NoClassDefFoundError} at class-load time when {@code spring-jdbc} is absent.
	 */
	private static final class SpringJdbcDelegate {

		static boolean containsProxiedTarget(final DataSource ds) {
			if (ds instanceof DelegatingDataSource delegating) {
				final DataSource target = delegating.getTargetDataSource();
				return target != null
						&& (target instanceof ProxyJdbcObject || containsProxiedTarget(target));
			}
			if (ds instanceof AbstractRoutingDataSource routing) {
				return routing.getResolvedDataSources()
					.values()
					.stream()
					.anyMatch(target -> target instanceof ProxyJdbcObject || containsProxiedTarget(target));
			}
			return false;
		}

	}

}
