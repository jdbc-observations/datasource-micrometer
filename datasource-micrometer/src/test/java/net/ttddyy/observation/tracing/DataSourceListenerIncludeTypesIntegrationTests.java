/*
 * Copyright 2022-2023 the original author or authors.
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

package net.ttddyy.observation.tracing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.test.simple.SimpleSpan;
import io.micrometer.tracing.test.simple.SimpleTracer;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tadaya Tsuyukubo
 */
class DataSourceListenerIncludeTypesIntegrationTests {

	private static JdbcDataSource dataSource;

	@BeforeAll
	static void setUpDataSource() throws Exception {
		dataSource = createDataSource();
		executeQuery(dataSource, "CREATE TABLE emp(id INT, name VARCHAR(20))");
		executeQuery(dataSource, "INSERT INTO emp VALUES (10, 'Foo')");
		executeQuery(dataSource, "INSERT INTO emp VALUES (20, 'Bar')");
		executeQuery(dataSource, "INSERT INTO emp VALUES (30, 'Baz')");
	}

	private static JdbcDataSource createDataSource() {
		JdbcDataSource ds = new JdbcDataSource();
		ds.setURL("jdbc:h2:mem:mytest;DB_CLOSE_DELAY=-1");
		ds.setUser("sa");
		ds.setPassword("sa");
		return ds;
	}

	private static void executeQuery(DataSource ds, String query) throws Exception {
		try (Connection conn = ds.getConnection()) {
			try (Statement stmt = conn.createStatement()) {
				stmt.execute(query);
			}
		}
	}

	@AfterAll
	static void shutdownDataBase() throws Exception {
		executeQuery(dataSource, "SHUTDOWN");
	}

	@ParameterizedTest
	@MethodSource
	void supportedTypes(List<JdbcObservationDocumentation> supportedTypeLists) throws Exception {
		Set<JdbcObservationDocumentation> supportedTypes = new HashSet<>(supportedTypeLists);
		Set<String> expectedSpanNames = supportedTypes.stream().map(JdbcObservationDocumentation::getContextualName)
				.collect(Collectors.toSet());

		ObservationRegistry registry = ObservationRegistry.create();
		SimpleTracer tracer = new SimpleTracer();
		registry.observationConfig().observationHandler(new ConnectionTracingObservationHandler(tracer));
		registry.observationConfig().observationHandler(new QueryTracingObservationHandler(tracer));
		registry.observationConfig().observationHandler(new ResultSetTracingObservationHandler(tracer));
		DataSourceObservationListener listener = new DataSourceObservationListener(registry);
		listener.setSupportedTypes(supportedTypes);

		ProxyDataSourceBuilder builder = ProxyDataSourceBuilder.create(dataSource).listener(listener).name("proxy-ds")
				.methodListener(listener);
		builder.proxyResultSet(); // enable ResultSet observation
		builder.name("proxy"); // translates to the service name
		DataSource proxyDataSource = builder.build();

		String result = doLogic(proxyDataSource);
		assertThat(result).isEqualTo("Bar");
		assertThat(tracer.getSpans()).extracting(SimpleSpan::getName)
				.containsExactlyInAnyOrderElementsOf(expectedSpanNames);

		// make sure, observation scope is closed.
		assertThat(registry.getCurrentObservation()).isNull();
	}

	private String doLogic(DataSource ds) throws Exception {
		try (Connection conn = ds.getConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement("SELECT name FROM emp WHERE id = ?")) {
				stmt.setInt(1, 20);
				try (ResultSet rs = stmt.executeQuery()) {
					rs.next();
					return rs.getString(1);
				}
			}
		}
	}

	static Stream<Arguments> supportedTypes() {
		// @formatter:off
		return Stream.of(
				Arguments.of(Arrays.asList(JdbcObservationDocumentation.values())),
				Arguments.of(Arrays.asList(JdbcObservationDocumentation.CONNECTION)),
				Arguments.of(Arrays.asList(JdbcObservationDocumentation.QUERY)),
				Arguments.of(Arrays.asList(JdbcObservationDocumentation.RESULT_SET)),
				Arguments.of(Arrays.asList(JdbcObservationDocumentation.CONNECTION, JdbcObservationDocumentation.QUERY)),
				Arguments.of(Arrays.asList(JdbcObservationDocumentation.CONNECTION, JdbcObservationDocumentation.QUERY, JdbcObservationDocumentation.RESULT_SET)),
				Arguments.of(Arrays.asList(JdbcObservationDocumentation.CONNECTION, JdbcObservationDocumentation.RESULT_SET)),
				Arguments.of(Arrays.asList(JdbcObservationDocumentation.QUERY, JdbcObservationDocumentation.RESULT_SET)),
				Arguments.of(Arrays.asList())
				);
		// @formatter:on
	}

}
