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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

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

/**
 * A {@link BeanPostProcessor} to instrument {@link DataSource} beans.
 *
 * @author Tadaya Tsuyukubo
 */
public class DataSourceObservationBeanPostProcessor implements BeanPostProcessor {

	/**
	 * Tracks datasource instances (pre-proxy) that this post-processor has already
	 * instrumented, so routing/delegating wrappers referencing them can be skipped.
	 */
	private final Set<DataSource> proxiedDataSources = Collections.newSetFromMap(new IdentityHashMap<>());

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
			Object proxy;
			if (dataSourceType == DataSourceType.PROXY || dataSourceType == DataSourceType.SPRING_PROXY) {
				proxy = builder.buildProxy();
			}
			else {
				proxy = builder.build();
			}
			this.proxiedDataSources.add(dataSource);
			return proxy;
		}
		else {
			return bean;
		}
	}

	/**
	 * Returns {@code true} if {@code ds} is a routing or delegating wrapper whose chain
	 * contains a datasource that this post-processor has already instrumented. Detected
	 * via reflection to avoid a compile-time dependency on spring-jdbc.
	 *
	 * <p>Two common patterns are checked:
	 * <ul>
	 * <li>{@code getTargetDataSource()} — covers {@code DelegatingDataSource} (e.g.
	 * {@code LazyConnectionDataSourceProxy})</li>
	 * <li>{@code resolvedDataSources} field — covers {@code AbstractRoutingDataSource}
	 * subclasses</li>
	 * </ul>
	 */
	private boolean containsAlreadyProxiedTarget(DataSource ds) {
		// DelegatingDataSource pattern: getTargetDataSource()
		try {
			Method m = ds.getClass().getMethod("getTargetDataSource");
			Object target = m.invoke(ds);
			if (target instanceof DataSource targetDs) {
				return this.proxiedDataSources.contains(targetDs) || isProxyJdbcObject(targetDs)
						|| containsAlreadyProxiedTarget(targetDs);
			}
		}
		catch (ReflectiveOperationException ignored) {
		}

		// AbstractRoutingDataSource pattern: resolvedDataSources field
		Class<?> cls = ds.getClass();
		while (cls != null && cls != Object.class) {
			try {
				Field f = cls.getDeclaredField("resolvedDataSources");
				f.setAccessible(true);
				@SuppressWarnings("unchecked")
				Map<Object, DataSource> resolved = (Map<Object, DataSource>) f.get(ds);
				if (resolved != null) {
					return resolved.values()
						.stream()
						.anyMatch(t -> this.proxiedDataSources.contains(t) || isProxyJdbcObject(t)
								|| containsAlreadyProxiedTarget(t));
				}
				break;
			}
			catch (NoSuchFieldException e) {
				cls = cls.getSuperclass();
			}
			catch (IllegalAccessException ignored) {
				break;
			}
		}
		return false;
	}

	private static boolean isProxyJdbcObject(DataSource ds) {
		return ds instanceof ProxyJdbcObject;
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

}
