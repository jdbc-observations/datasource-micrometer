/*
 * Copyright 2022 the original author or authors.
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
import net.ttddyy.dsproxy.proxy.ResultSetProxyLogicFactory;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import net.ttddyy.dsproxy.transform.ParameterTransformer;
import net.ttddyy.dsproxy.transform.QueryTransformer;

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

	private final ObjectProvider<JdbcProperties> jdbcPropertiesProvider;

	private final ObjectProvider<DataSourceNameResolver> dataSourceNameResolverProvider;

	private final ObjectProvider<QueryExecutionListener> listenersProvider;

	private final ObjectProvider<MethodExecutionListener> methodExecutionListenersProvider;

	private final ObjectProvider<ParameterTransformer> parameterTransformerProvider;

	private final ObjectProvider<QueryTransformer> queryTransformerProvider;

	private final ObjectProvider<ResultSetProxyLogicFactory> resultSetProxyLogicFactoryProvider;

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
			ObjectProvider<DataSourceProxyConnectionIdManagerProvider> dataSourceProxyConnectionIdManagerProviderProvider,
			ObjectProvider<ProxyDataSourceBuilderCustomizer> proxyDataSourceBuilderCustomizers) {
		this.jdbcPropertiesProvider = jdbcPropertiesProvider;
		this.dataSourceNameResolverProvider = dataSourceNameResolverProvider;
		this.listenersProvider = listenersProvider;
		this.methodExecutionListenersProvider = methodExecutionListenersProvider;
		this.parameterTransformerProvider = parameterTransformerProvider;
		this.queryTransformerProvider = queryTransformerProvider;
		this.resultSetProxyLogicFactoryProvider = resultSetProxyLogicFactoryProvider;
		this.dataSourceProxyConnectionIdManagerProviderProvider = dataSourceProxyConnectionIdManagerProviderProvider;
		this.proxyDataSourceBuilderCustomizers = proxyDataSourceBuilderCustomizers;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof DataSource dataSource && !ScopedProxyUtils.isScopedTarget(beanName)
				&& !isExcludedBean(beanName)) {
			String dataSourceName = this.dataSourceNameResolverProvider.getObject().resolve(beanName, dataSource);
			ProxyDataSourceBuilder builder = ProxyDataSourceBuilder.create(dataSourceName, dataSource);
			getConfigurer().configure(builder);
			this.proxyDataSourceBuilderCustomizers.orderedStream()
					.forEach(customizer -> customizer.customize(builder, dataSource, beanName, dataSourceName));
			return builder.build();
		}
		else {
			return bean;
		}
	}

	private DataSourceProxyBuilderConfigurer getConfigurer() {
		if (this.dataSourceProxyBuilderConfigurer == null) {
			this.dataSourceProxyBuilderConfigurer = new DataSourceProxyBuilderConfigurer(getJdbcProperties(),
					this.listenersProvider.orderedStream().toList(),
					this.methodExecutionListenersProvider.orderedStream().toList(),
					this.parameterTransformerProvider.getIfAvailable(), this.queryTransformerProvider.getIfAvailable(),
					this.resultSetProxyLogicFactoryProvider.getIfAvailable(),
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
