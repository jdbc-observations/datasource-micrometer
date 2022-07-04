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

import java.util.List;

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
 * @author Tadaya Tsuyukubo
 */
public class DataSourceObservationBeanPostProcessor implements BeanPostProcessor {

	private final ObjectProvider<JdbcProperties> jdbcPropertiesProvider;

	private final ObjectProvider<DataSourceNameResolver> dataSourceNameResolverProvider;

	private final ObjectProvider<List<QueryExecutionListener>> listenersProvider;

	private final ObjectProvider<List<MethodExecutionListener>> methodExecutionListenersProvider;

	private final ObjectProvider<ParameterTransformer> parameterTransformerProvider;

	private final ObjectProvider<QueryTransformer> queryTransformerProvider;

	private final ObjectProvider<ResultSetProxyLogicFactory> resultSetProxyLogicFactoryProvider;

	private final ObjectProvider<DataSourceProxyConnectionIdManagerProvider> dataSourceProxyConnectionIdManagerProviderProvider;

	private DataSourceProxyBuilderConfigurer dataSourceProxyBuilderConfigurer;

	public DataSourceObservationBeanPostProcessor(
			ObjectProvider<JdbcProperties> jdbcPropertiesProvider,
			ObjectProvider<DataSourceNameResolver> dataSourceNameResolverProvider,
			ObjectProvider<List<QueryExecutionListener>> listenersProvider,
			ObjectProvider<List<MethodExecutionListener>> methodExecutionListenersProvider,
			ObjectProvider<ParameterTransformer> parameterTransformerProvider,
			ObjectProvider<QueryTransformer> queryTransformerProvider,
			ObjectProvider<ResultSetProxyLogicFactory> resultSetProxyLogicFactoryProvider,
			ObjectProvider<DataSourceProxyConnectionIdManagerProvider> dataSourceProxyConnectionIdManagerProviderProvider
	) {
		this.jdbcPropertiesProvider = jdbcPropertiesProvider;
		this.dataSourceNameResolverProvider = dataSourceNameResolverProvider;
		this.listenersProvider = listenersProvider;
		this.methodExecutionListenersProvider = methodExecutionListenersProvider;
		this.parameterTransformerProvider = parameterTransformerProvider;
		this.queryTransformerProvider = queryTransformerProvider;
		this.resultSetProxyLogicFactoryProvider = resultSetProxyLogicFactoryProvider;
		this.dataSourceProxyConnectionIdManagerProviderProvider = dataSourceProxyConnectionIdManagerProviderProvider;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof DataSource dataSource && !ScopedProxyUtils.isScopedTarget(beanName)
				&& !isExcludedBean(beanName)) {
			String dataSourceName = this.dataSourceNameResolverProvider.getObject().resolve(beanName, dataSource);
			ProxyDataSourceBuilder builder = getConfigurer().configure(ProxyDataSourceBuilder.create());
			return builder.name(dataSourceName).dataSource(dataSource).build();
		}
		else {
			return bean;
		}
	}

	private DataSourceProxyBuilderConfigurer getConfigurer() {
		if (this.dataSourceProxyBuilderConfigurer == null) {
			this.dataSourceProxyBuilderConfigurer = new DataSourceProxyBuilderConfigurer(getJdbcProperties(),
					this.listenersProvider.getIfAvailable(), this.methodExecutionListenersProvider.getIfAvailable(),
					this.parameterTransformerProvider.getIfAvailable(), this.queryTransformerProvider.getIfAvailable(),
					this.resultSetProxyLogicFactoryProvider.getIfAvailable(), this.dataSourceProxyConnectionIdManagerProviderProvider.getIfAvailable());
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
