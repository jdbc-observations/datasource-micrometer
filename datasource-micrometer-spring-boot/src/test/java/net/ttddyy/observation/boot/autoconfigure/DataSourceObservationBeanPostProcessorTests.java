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

import java.sql.Connection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.sql.DataSource;

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

import org.springframework.aop.SpringProxy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

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

		this.processor = new DataSourceObservationBeanPostProcessor(this.jdbcPropertiesProvider,
				this.dataSourceNameResolverProvider, this.listenersProvider, this.methodExecutionListenersProvider,
				this.parameterTransformerProvider, this.queryTransformerProvider,
				this.resultSetProxyLogicFactoryProvider, this.generatedKeysProxyLogicFactoryProvider,
				this.dataSourceProxyConnectionIdManagerProviderProvider, this.proxyDataSourceBuilderCustomizers);
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

	@Test
	void proxyDataSourceBuilderCustomizers() {
		JdbcProperties jdbcProperties = new JdbcProperties();
		given(this.jdbcPropertiesProvider.getObject()).willReturn(jdbcProperties);

		DataSourceNameResolver dataSourceNameResolver = mock(DataSourceNameResolver.class);
		given(dataSourceNameResolver.resolve(any(String.class), any(DataSource.class))).willReturn("not-customized-ds");
		given(this.dataSourceNameResolverProvider.getObject()).willReturn(dataSourceNameResolver);

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

	@Test
	void delegatingDataSourceWrappingAlreadyProxiedTargetIsSkipped() throws Exception {
		setupProcessorForProxying();

		// First: proxy the physical datasource
		DataSource physical = mockPhysicalDataSource();
		Object proxied = this.processor.postProcessAfterInitialization(physical, "physicalDataSource");
		assertThat(proxied).isInstanceOf(ProxyJdbcObject.class);

		// Then: wrap the proxy in a LazyConnectionDataSourceProxy (DelegatingDataSource)
		LazyConnectionDataSourceProxy lazy = new LazyConnectionDataSourceProxy((DataSource) proxied);

		// The delegating wrapper should NOT be double-proxied
		Object result = this.processor.postProcessAfterInitialization(lazy, "actualDataSource");
		assertThat(result).isNotInstanceOf(ProxyJdbcObject.class)
			.isInstanceOf(LazyConnectionDataSourceProxy.class);
	}

	@Test
	void routingDataSourceWrappingAlreadyProxiedTargetsIsSkipped() throws Exception {
		setupProcessorForProxying();

		// First: proxy both physical datasources
		DataSource physicalRw = mockPhysicalDataSource();
		DataSource physicalRo = mockPhysicalDataSource();
		DataSource proxiedRw = (DataSource) this.processor.postProcessAfterInitialization(physicalRw,
				"readWriteDataSource");
		DataSource proxiedRo = (DataSource) this.processor.postProcessAfterInitialization(physicalRo,
				"readOnlyDataSource");

		// Then: build an AbstractRoutingDataSource pointing at the already-proxied pools
		AbstractRoutingDataSource router = new AbstractRoutingDataSource() {
			@Override
			protected Object determineCurrentLookupKey() {
				return "rw";
			}
		};
		router.setTargetDataSources(Map.of("rw", proxiedRw, "ro", proxiedRo));
		router.setDefaultTargetDataSource(proxiedRw);
		router.afterPropertiesSet();

		// The routing datasource should NOT be double-proxied
		Object result = this.processor.postProcessAfterInitialization(router, "actualDataSource");
		assertThat(result).isNotInstanceOf(ProxyJdbcObject.class)
			.isInstanceOf(AbstractRoutingDataSource.class);
	}

	@Test
	void nestedDelegatingChainWrappingAlreadyProxiedTargetIsSkipped() throws Exception {
		setupProcessorForProxying();

		DataSource physical = mockPhysicalDataSource();
		DataSource proxied = (DataSource) this.processor.postProcessAfterInitialization(physical, "physical");

		// Build: LazyConnectionDataSourceProxy → AbstractRoutingDataSource → proxied
		AbstractRoutingDataSource router = new AbstractRoutingDataSource() {
			@Override
			protected Object determineCurrentLookupKey() {
				return "default";
			}
		};
		router.setTargetDataSources(Map.of("default", proxied));
		router.setDefaultTargetDataSource(proxied);
		router.afterPropertiesSet();

		LazyConnectionDataSourceProxy lazy = new LazyConnectionDataSourceProxy(router);

		// Neither the router nor the lazy wrapper should be double-proxied
		Object routerResult = this.processor.postProcessAfterInitialization(router, "router");
		assertThat(routerResult).isNotInstanceOf(ProxyJdbcObject.class);

		Object lazyResult = this.processor.postProcessAfterInitialization(lazy, "lazy");
		assertThat(lazyResult).isNotInstanceOf(ProxyJdbcObject.class);
	}

	@Test
	void independentDataSourceIsStillProxied() throws Exception {
		setupProcessorForProxying();

		DataSource first = mockPhysicalDataSource();
		DataSource second = mockPhysicalDataSource();

		Object firstResult = this.processor.postProcessAfterInitialization(first, "firstDataSource");
		Object secondResult = this.processor.postProcessAfterInitialization(second, "secondDataSource");

		assertThat(firstResult).isInstanceOf(ProxyJdbcObject.class);
		assertThat(secondResult).isInstanceOf(ProxyJdbcObject.class);
	}

	private void setupProcessorForProxying() {
		JdbcProperties jdbcProperties = new JdbcProperties();
		given(this.jdbcPropertiesProvider.getObject()).willReturn(jdbcProperties);
		given(this.dataSourceNameResolverProvider.getObject()).willReturn(new DefaultDataSourceNameResolver());
		// Use willAnswer so each call gets a fresh stream (streams are single-use)
		given(this.proxyDataSourceBuilderCustomizers.orderedStream()).willAnswer(inv -> Stream.of());
	}

	private static DataSource mockPhysicalDataSource() throws Exception {
		Connection connection = mock(Connection.class);
		DataSource dataSource = mock(DataSource.class);
		given(dataSource.getConnection()).willReturn(connection);
		return dataSource;
	}

}
