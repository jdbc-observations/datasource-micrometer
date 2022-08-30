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

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.sql.DataSource;

import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.listener.CompositeMethodListener;
import net.ttddyy.dsproxy.listener.MethodExecutionListener;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.proxy.ProxyConfig;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.observation.tracing.ConnectionObservationConvention;
import net.ttddyy.observation.tracing.ConnectionTracingObservationHandler;
import net.ttddyy.observation.tracing.DataSourceBaseObservationHandler;
import net.ttddyy.observation.tracing.DataSourceObservationListener;
import net.ttddyy.observation.tracing.QueryObservationConvention;
import net.ttddyy.observation.tracing.QueryTracingObservationHandler;
import net.ttddyy.observation.tracing.ResultSetObservationConvention;
import net.ttddyy.observation.tracing.ResultSetTracingObservationHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DataSourceObservationAutoConfiguration}.
 *
 * @author Tadaya Tsuyukubo
 */
class DataSourceObservationAutoConfigurationTests {

	@Test
	void defaultEnabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(DataSourceObservationAutoConfiguration.class))
				.withBean(ObservationRegistry.class, ObservationRegistry::create)
				.withBean(Tracer.class, () -> mock(Tracer.class)).run((context) -> {
					assertThat(context).hasSingleBean(DataSourceObservationAutoConfiguration.class);
				});
	}

	@Test
	void disabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(DataSourceObservationAutoConfiguration.class))
				.withPropertyValues("jdbc.datasource-proxy.enabled=false").run((context) -> {
					assertThat(context).doesNotHaveBean(DataSourceObservationAutoConfiguration.class);
				});
	}

	@Test
	void customObservationConventions() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(DataSourceObservationAutoConfiguration.class))
				.withBean(ObservationRegistry.class, ObservationRegistry::create)
				.withBean(Tracer.class, () -> mock(Tracer.class)).withBean(CustomConnectionObservationConvention.class)
				.withBean(CustomQueryObservationConvention.class).withBean(CustomResultSetObservationConvention.class)
				.run((context) -> {
					assertThat(context).getBean(DataSourceObservationListener.class).satisfies((listener) -> {
						assertThat(listener).extracting("connectionObservationConvention")
								.isInstanceOf(CustomConnectionObservationConvention.class);
						assertThat(listener).extracting("queryObservationConvention")
								.isInstanceOf(CustomQueryObservationConvention.class);
						assertThat(listener).extracting("resultSetObservationConvention")
								.isInstanceOf(CustomResultSetObservationConvention.class);
					});
				});
	}

	@Test
	void customDataSourceProxyListeners() {
		QueryExecutionListener queryListenerA = mock(QueryExecutionListener.class);
		QueryExecutionListener queryListenerB = mock(QueryExecutionListener.class);
		MethodExecutionListener methodListenerA = mock(MethodExecutionListener.class);
		MethodExecutionListener methodListenerB = mock(MethodExecutionListener.class);
		DataSource dataSource = mock(DataSource.class);

		// @formatter:off
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(DataSourceObservationAutoConfiguration.class))
				.withBean(ObservationRegistry.class, ObservationRegistry::create)
				.withBean(Tracer.class, () -> mock(Tracer.class))
				.withBean(DataSource.class, () -> dataSource)
				.withBean("queryListenerA", QueryExecutionListener.class, () -> queryListenerA)
				.withBean("queryListenerB", QueryExecutionListener.class, () -> queryListenerB)
				.withBean("methodListenerB", MethodExecutionListener.class, () -> methodListenerA)
				.withBean("methodListenerA", MethodExecutionListener.class, () -> methodListenerB)
				.run((context) -> {
					assertThat(context).hasNotFailed();
					DataSource ds = context.getBean(DataSource.class);
					assertThat(ds).isInstanceOfSatisfying(ProxyDataSource.class, proxy -> {
						ProxyConfig proxyConfig = proxy.getProxyConfig();
						ChainListener queryListeners = proxyConfig.getQueryListener();
						assertThat(queryListeners.getListeners()).contains(queryListenerA, queryListenerB);

						CompositeMethodListener methodListeners = proxyConfig.getMethodListener();
						assertThat(methodListeners.getListeners()).contains(methodListenerA, methodListenerB);
					});
				});
		// @formatter:on
	}

	@Test
	void observationHandler() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(DataSourceObservationAutoConfiguration.class))
				.withPropertyValues("management.tracing.enabled=true")
				.withBean(ObservationRegistry.class, ObservationRegistry::create)
				.withBean(Tracer.class, () -> mock(Tracer.class)).run((context) -> {
					assertThat(context).getBean(ConnectionTracingObservationHandler.class);
					assertThat(context).getBean(QueryTracingObservationHandler.class);
					assertThat(context).getBean(ResultSetTracingObservationHandler.class);
				});
	}

	@Test
	void includeNotSpecified() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(DataSourceObservationAutoConfiguration.class))
				.withBean(ObservationRegistry.class, ObservationRegistry::create)
				.withBean(Tracer.class, () -> mock(Tracer.class)).run((context) -> {
					assertThat(context).hasSingleBean(ConnectionTracingObservationHandler.class)
							.hasSingleBean(QueryTracingObservationHandler.class)
							.hasSingleBean(ResultSetTracingObservationHandler.class);
				});
	}

	@ParameterizedTest
	@MethodSource
	void includeTypes(String property, Set<Class<? extends DataSourceBaseObservationHandler>> handlers) {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(DataSourceObservationAutoConfiguration.class))
				.withPropertyValues("management.tracing.enabled=true").withPropertyValues(property)
				.withBean(ObservationRegistry.class, ObservationRegistry::create)
				.withBean(Tracer.class, () -> mock(Tracer.class)).run((context) -> {
					assertThat(context).getBeans(ObservationHandler.class).extracting(Map::values)
							.satisfies((beans) -> {
								assertThat(beans).extracting(Object::getClass).allMatch(handlers::contains);
							});
				});
	}

	static Stream<Arguments> includeTypes() {
		Class<?> connection = ConnectionTracingObservationHandler.class;
		Class<?> query = QueryTracingObservationHandler.class;
		Class<?> resultSet = ResultSetTracingObservationHandler.class;
		return Stream.of(Arguments.of("jdbc.includes=CONNECTION", Set.of(connection)),
				Arguments.of("jdbc.includes=QUERY", Set.of(query)),
				Arguments.of("jdbc.includes=FETCH", Set.of(resultSet)),
				Arguments.of("jdbc.includes=CONNECTION,FETCH", Set.of(connection, resultSet)),
				Arguments.of("jdbc.includes=CONNECTION,QUERY, FETCH", Set.of(connection, query, resultSet)));
	}

	static class CustomConnectionObservationConvention implements ConnectionObservationConvention {

	}

	static class CustomQueryObservationConvention implements QueryObservationConvention {

	}

	static class CustomResultSetObservationConvention implements ResultSetObservationConvention {

	}

}
