package net.ttddyy.observation.boot.autoconfigure;

import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import net.ttddyy.dsproxy.listener.MethodExecutionListener;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.proxy.ResultSetProxyLogicFactory;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.transform.ParameterTransformer;
import net.ttddyy.dsproxy.transform.QueryTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DataSourceObservationBeanPostProcessor}.
 *
 * @author Tadaya Tsuyukubo
 */
class DataSourceObservationBeanPostProcessorTests {

	private ObjectProvider<JdbcProperties> jdbcPropertiesProvider;

	private ObjectProvider<DataSourceNameResolver> dataSourceNameResolverProvider;

	private ObjectProvider<List<QueryExecutionListener>> listenersProvider;

	private ObjectProvider<List<MethodExecutionListener>> methodExecutionListenersProvider;

	private ObjectProvider<ParameterTransformer> parameterTransformerProvider;

	private ObjectProvider<QueryTransformer> queryTransformerProvider;

	private ObjectProvider<ResultSetProxyLogicFactory> resultSetProxyLogicFactoryProvider;

	private ObjectProvider<DataSourceProxyConnectionIdManagerProvider> dataSourceProxyConnectionIdManagerProviderProvider;

	private DataSourceObservationBeanPostProcessor processor;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void beforeEach() {
		this.jdbcPropertiesProvider = mock(ObjectProvider.class);
		this.dataSourceNameResolverProvider = mock(ObjectProvider.class);
		this.listenersProvider = mock(ObjectProvider.class);
		this.methodExecutionListenersProvider = mock(ObjectProvider.class);
		this.parameterTransformerProvider = mock(ObjectProvider.class);
		this.queryTransformerProvider = mock(ObjectProvider.class);
		this.resultSetProxyLogicFactoryProvider = mock(ObjectProvider.class);
		this.dataSourceProxyConnectionIdManagerProviderProvider = mock(ObjectProvider.class);

		this.processor = new DataSourceObservationBeanPostProcessor(this.jdbcPropertiesProvider,
				this.dataSourceNameResolverProvider, this.listenersProvider, this.methodExecutionListenersProvider,
				this.parameterTransformerProvider, this.queryTransformerProvider, this.resultSetProxyLogicFactoryProvider,
				this.dataSourceProxyConnectionIdManagerProviderProvider);
	}

	@Test
	void postProcessAfterInitialization() {
		JdbcProperties jdbcProperties = new JdbcProperties();
		given(this.jdbcPropertiesProvider.getObject()).willReturn(jdbcProperties);

		DataSourceNameResolver dataSourceNameResolver = new DefaultDataSourceNameResolver();
		given(this.dataSourceNameResolverProvider.getObject()).willReturn(dataSourceNameResolver);

		DataSource dataSource = mock(DataSource.class);
		Object result = this.processor.postProcessAfterInitialization(dataSource, "foo");

		assertThat(result).isInstanceOf(ProxyDataSource.class);

	}

	@Test
	void excludedDataSourceBeanNames() {
		JdbcProperties jdbcProperties = new JdbcProperties();
		jdbcProperties.setExcludedDataSourceBeanNames(Set.of("foo"));
		given(this.jdbcPropertiesProvider.getObject()).willReturn(jdbcProperties);

		DataSourceNameResolver dataSourceNameResolver = new DefaultDataSourceNameResolver();
		given(this.dataSourceNameResolverProvider.getObject()).willReturn(dataSourceNameResolver);

		DataSource dataSource = mock(DataSource.class);
		Object result = this.processor.postProcessAfterInitialization(dataSource, "foo");

		assertThat(result).isInstanceOf(DataSource.class).isNotInstanceOf(ProxyDataSource.class);
	}

}
