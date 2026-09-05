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

import net.ttddyy.dsproxy.listener.MethodExecutionListener;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.proxy.ProxyJdbcObject;
import net.ttddyy.dsproxy.proxy.ResultSetProxyLogicFactory;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.transform.ParameterTransformer;
import net.ttddyy.dsproxy.transform.QueryTransformer;
import net.ttddyy.observation.boot.autoconfigure.JdbcProperties.DataSourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DataSourceObservationBeanPostProcessor}.
 *
 * @author Tadaya Tsuyukubo
 */
class DataSourceObservationBeanPostProcessorTests {

	private ObjectProvider<JdbcProperties> jdbcPropertiesProvider;

	private ObjectProvider<DataSourceNameResolver> dataSourceNameResolverProvider;

	private ObjectProvider<QueryExecutionListener> listenersProvider;

	private ObjectProvider<MethodExecutionListener> methodExecutionListenersProvider;

	private ObjectProvider<ParameterTransformer> parameterTransformerProvider;

	private ObjectProvider<QueryTransformer> queryTransformerProvider;

	private ObjectProvider<ResultSetProxyLogicFactory> resultSetProxyLogicFactoryProvider;

	private ObjectProvider<ResultSetProxyLogicFactory> generatedKeysProxyLogicFactoryProvider;

	private ObjectProvider<DataSourceProxyConnectionIdManagerProvider> dataSourceProxyConnectionIdManagerProviderProvider;

	private ObjectProvider<ProxyDataSourceBuilderCustomizer> proxyDataSourceBuilderCustomizers;

	private ObjectProvider<DataSourceProxyCreationStrategy> dataSourceProxyCreationStrategy;

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
		this.generatedKeysProxyLogicFactoryProvider = mock(ObjectProvider.class);
		this.dataSourceProxyConnectionIdManagerProviderProvider = mock(ObjectProvider.class);
		this.proxyDataSourceBuilderCustomizers = mock(ObjectProvider.class);
		this.dataSourceProxyCreationStrategy = mock(ObjectProvider.class);

		this.processor = new DataSourceObservationBeanPostProcessor(this.jdbcPropertiesProvider,
				this.dataSourceNameResolverProvider, this.listenersProvider, this.methodExecutionListenersProvider,
				this.parameterTransformerProvider, this.queryTransformerProvider,
				this.resultSetProxyLogicFactoryProvider, this.generatedKeysProxyLogicFactoryProvider,
				this.dataSourceProxyConnectionIdManagerProviderProvider, this.proxyDataSourceBuilderCustomizers,
				this.dataSourceProxyCreationStrategy);
	}

	@Test
	void postProcessAfterInitialization() throws Exception {
		JdbcProperties jdbcProperties = new JdbcProperties();
		given(this.jdbcPropertiesProvider.getObject()).willReturn(jdbcProperties);

		DataSourceNameResolver dataSourceNameResolver = new DefaultDataSourceNameResolver();
		given(this.dataSourceNameResolverProvider.getObject()).willReturn(dataSourceNameResolver);

		given(this.proxyDataSourceBuilderCustomizers.orderedStream()).willReturn(Stream.of());

		// the DefaultDataSourceNameResolver retrieves catalog from connection
		Connection connection = mock(Connection.class);
		DataSource dataSource = mock(DataSource.class);
		given(dataSource.getConnection()).willReturn(connection);

		DataSourceProxyCreationStrategy creationStrategy = mock(DataSourceProxyCreationStrategy.class);
		given(creationStrategy.shouldCreateProxy(any(DataSource.class), any(String.class))).willReturn(true);
		given(this.dataSourceProxyCreationStrategy.getObject()).willReturn(creationStrategy);

		Object result = this.processor.postProcessAfterInitialization(dataSource, "foo");

		assertThat(result).isInstanceOf(DataSource.class).isInstanceOf(ProxyJdbcObject.class);
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

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldCreateProxy(boolean shouldCreateProxy) {
		JdbcProperties jdbcProperties = new JdbcProperties();
		given(this.jdbcPropertiesProvider.getObject()).willReturn(jdbcProperties);

		DataSourceNameResolver dataSourceNameResolver = mock(DataSourceNameResolver.class);
		given(dataSourceNameResolver.resolve(any(String.class), any(DataSource.class))).willReturn("my-ds");
		given(this.dataSourceNameResolverProvider.getObject()).willReturn(dataSourceNameResolver);

		DataSourceProxyCreationStrategy creationStrategy = mock(DataSourceProxyCreationStrategy.class);
		given(creationStrategy.shouldCreateProxy(any(DataSource.class), any(String.class)))
			.willReturn(shouldCreateProxy);
		given(this.dataSourceProxyCreationStrategy.getObject()).willReturn(creationStrategy);

		DataSource dataSource = mock(DataSource.class);
		Object result = this.processor.postProcessAfterInitialization(dataSource, "foo");

		if (shouldCreateProxy) {
			assertThat(result).isNotSameAs(dataSource)
				.isInstanceOf(DataSource.class)
				.isInstanceOf(ProxyJdbcObject.class);
		}
		else {
			assertThat(result).isSameAs(dataSource);
		}
	}

	@Test
	void proxyDataSourceBuilderCustomizers() {
		JdbcProperties jdbcProperties = new JdbcProperties();
		given(this.jdbcPropertiesProvider.getObject()).willReturn(jdbcProperties);

		DataSourceNameResolver dataSourceNameResolver = mock(DataSourceNameResolver.class);
		given(dataSourceNameResolver.resolve(any(String.class), any(DataSource.class))).willReturn("not-customized-ds");
		given(this.dataSourceNameResolverProvider.getObject()).willReturn(dataSourceNameResolver);

		DataSourceProxyCreationStrategy creationStrategy = mock(DataSourceProxyCreationStrategy.class);
		given(creationStrategy.shouldCreateProxy(any(DataSource.class), any(String.class))).willReturn(true);
		given(this.dataSourceProxyCreationStrategy.getObject()).willReturn(creationStrategy);

		ProxyDataSourceBuilderCustomizer customizer = (builder, dataSource, beanName, dataSourceName) -> {
			assertThat(beanName).isEqualTo("foo");
			assertThat(dataSourceName).isEqualTo("not-customized-ds");
			builder.name("customized-ds");
		};

		given(this.proxyDataSourceBuilderCustomizers.orderedStream()).willReturn(Stream.of(customizer));

		DataSource dataSource = mock(DataSource.class);
		Object result = this.processor.postProcessAfterInitialization(dataSource, "foo");

		assertThat(result).isInstanceOfSatisfying(ProxyJdbcObject.class, (proxy) -> {
			assertThat(proxy.getProxyConfig().getDataSourceName()).isEqualTo("customized-ds");
		});

		verify(dataSourceNameResolver).resolve(any(), any());
	}

	@ParameterizedTest
	@EnumSource(DataSourceType.class)
	void dataSourceType(DataSourceType type) {
		JdbcProperties jdbcProperties = new JdbcProperties();
		jdbcProperties.getDatasourceProxy().setType(type);
		given(this.jdbcPropertiesProvider.getObject()).willReturn(jdbcProperties);

		DataSourceNameResolver dataSourceNameResolver = mock(DataSourceNameResolver.class);
		given(dataSourceNameResolver.resolve(any(String.class), any(DataSource.class))).willReturn("my-ds");
		given(this.dataSourceNameResolverProvider.getObject()).willReturn(dataSourceNameResolver);

		DataSourceProxyCreationStrategy creationStrategy = mock(DataSourceProxyCreationStrategy.class);
		given(creationStrategy.shouldCreateProxy(any(DataSource.class), any(String.class))).willReturn(true);
		given(this.dataSourceProxyCreationStrategy.getObject()).willReturn(creationStrategy);

		DataSource dataSource = mock(DataSource.class);
		Object result = this.processor.postProcessAfterInitialization(dataSource, "foo");

		if (type == DataSourceType.PROXY || type == DataSourceType.SPRING_PROXY) {
			assertThat(result).isInstanceOf(DataSource.class)
				.isInstanceOf(ProxyJdbcObject.class)
				.isNotInstanceOf(ProxyDataSource.class);
		}
		else if (type == DataSourceType.CONCRETE) {
			assertThat(result).isInstanceOf(DataSource.class)
				.isInstanceOf(ProxyDataSource.class)
				.isNotInstanceOf(ProxyJdbcObject.class);
		}
		else {
			fail("Not supported type: " + type);
		}
	}

}
