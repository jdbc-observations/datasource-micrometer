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
