/*
 * Copyright 2022-2024 the original author or authors.
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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.test.simple.SimpleSpan;
import io.micrometer.tracing.test.simple.SimpleTracer;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tadaya Tsuyukubo
 */
class DataSourceListenerIncludeTypesIntegrationTests {

	private static JdbcDataSource dataSource;

	@BeforeAll
	static void setUpDataSource() throws Exception {
		dataSource = createDataSource();
		executeQuery(dataSource, "CREATE TABLE emp(id IDENTITY NOT NULL PRIMARY KEY, name VARCHAR(20))");
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

	static abstract class Base {

		protected SimpleTracer tracer;

		protected ObservationRegistry registry;

		@BeforeEach
		void clear() throws Exception {
			executeQuery(dataSource, "DELETE FROM emp");
			executeQuery(dataSource, "ALTER TABLE emp ALTER COLUMN id RESTART WITH 10");

			this.tracer = new SimpleTracer();
			this.registry = createObservationRegistry(this.tracer);
		}

		protected ObservationRegistry createObservationRegistry(Tracer tracer) {
			ObservationRegistry registry = ObservationRegistry.create();
			registry.observationConfig().observationHandler(new ConnectionTracingObservationHandler(tracer));
			registry.observationConfig().observationHandler(new QueryTracingObservationHandler(tracer));
			registry.observationConfig().observationHandler(new ResultSetTracingObservationHandler(tracer));
			return registry;
		}

		protected DataSource createProxyDataSource(List<JdbcObservationDocumentation> supportedTypeLists,
				ObservationRegistry registry) {
			DataSourceObservationListener listener = new DataSourceObservationListener(registry);
			listener.setSupportedTypes(new HashSet<>(supportedTypeLists));

			ProxyDataSourceBuilder builder = ProxyDataSourceBuilder.create(dataSource).listener(listener)
					.name("proxy-ds").methodListener(listener);
			builder.proxyResultSet(); // enable ResultSet observation
			builder.proxyGeneratedKeys(); // enable Generated Keys observation
			builder.name("proxy"); // translates to the service name
			return builder.build();
		}

		protected void verifySpanNames(List<JdbcObservationDocumentation> supportedTypeLists) {
			Set<String> expectedSpanNames = supportedTypeLists.stream()
					.map(JdbcObservationDocumentation::getContextualName).collect(Collectors.toSet());
			assertThat(this.tracer.getSpans()).extracting(SimpleSpan::getName)
					.containsExactlyInAnyOrderElementsOf(expectedSpanNames);
		}

		protected int countTable() throws SQLException {
			try (Connection connection = dataSource.getConnection()) {
				try (PreparedStatement statement = connection.prepareStatement("SELECT count(*) FROM emp")) {
					try (ResultSet resultSet = statement.executeQuery()) {
						resultSet.next();
						return resultSet.getInt(1);
					}
				}
			}
		}

	}

	@Nested
	class InsertWithGeneratedKeys extends Base {

		@ParameterizedTest
		@ArgumentsSource(InsertWithGeneratedKeysDataProvider.class)
		void supportedTypes(List<JdbcObservationDocumentation> supportedTypeLists,
				List<JdbcObservationDocumentation> expectedTypes) throws Exception {
			DataSource proxyDataSource = createProxyDataSource(supportedTypeLists, this.registry);
			String result = doLogic(proxyDataSource);
			assertThat(result).isEqualTo("10");
			verifySpanNames(expectedTypes);
			// make sure, observation scope is closed.
			assertThat(this.registry.getCurrentObservation()).isNull();
		}

		private String doLogic(DataSource ds) throws Exception {
			try (Connection conn = ds.getConnection()) {
				try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO emp (name) VALUES (?)",
						Statement.RETURN_GENERATED_KEYS)) {
					stmt.setString(1, "FOO");
					stmt.executeUpdate();
					try (ResultSet rs = stmt.getGeneratedKeys()) {
						rs.next();
						return rs.getString(1);
					}
				}
			}
		}

	}

	static class InsertWithGeneratedKeysDataProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
			// @formatter:off
			return Stream.of(
					// input list, expected list
				Arguments.of(Arrays.asList(JdbcObservationDocumentation.values()),
					Arrays.asList(JdbcObservationDocumentation.CONNECTION, JdbcObservationDocumentation.QUERY, JdbcObservationDocumentation.GENERATED_KEYS)),
				Arguments.of(Arrays.asList(JdbcObservationDocumentation.CONNECTION), Arrays.asList(JdbcObservationDocumentation.CONNECTION)),
				Arguments.of(Arrays.asList(JdbcObservationDocumentation.QUERY), Arrays.asList(JdbcObservationDocumentation.QUERY)),
				Arguments.of(Arrays.asList(JdbcObservationDocumentation.RESULT_SET), Arrays.asList()),
				Arguments.of(Arrays.asList(JdbcObservationDocumentation.GENERATED_KEYS), Arrays.asList(JdbcObservationDocumentation.GENERATED_KEYS)),
				Arguments.of(Arrays.asList(), Arrays.asList())
			);
			// @formatter:on
		}

	}

	@Nested
	class Insert extends Base {

		@ParameterizedTest
		@ArgumentsSource(InsertDataProvider.class)
		void supportedTypes(List<JdbcObservationDocumentation> supportedTypeLists,
				List<JdbcObservationDocumentation> expectedTypes) throws Exception {
			DataSource proxyDataSource = createProxyDataSource(supportedTypeLists, this.registry);
			doLogic(proxyDataSource);

			int count = countTable();
			assertThat(count).isEqualTo(1);
			verifySpanNames(expectedTypes);
			// make sure, observation scope is closed.
			assertThat(this.registry.getCurrentObservation()).isNull();
		}

		private void doLogic(DataSource ds) throws Exception {
			try (Connection conn = ds.getConnection()) {
				try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO emp (id, name) VALUES (?, ?)")) {
					stmt.setInt(1, 99);
					stmt.setString(2, "FOO");
					stmt.executeUpdate();
				}
			}
		}

	}

	static class InsertDataProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
			// @formatter:off
			return Stream.of(
					// input list, expected list
					Arguments.of(Arrays.asList(JdbcObservationDocumentation.values()),
							Arrays.asList(JdbcObservationDocumentation.CONNECTION, JdbcObservationDocumentation.QUERY)),
					Arguments.of(Arrays.asList(JdbcObservationDocumentation.CONNECTION), Arrays.asList(JdbcObservationDocumentation.CONNECTION)),
					Arguments.of(Arrays.asList(JdbcObservationDocumentation.QUERY), Arrays.asList(JdbcObservationDocumentation.QUERY)),
					Arguments.of(Arrays.asList(JdbcObservationDocumentation.RESULT_SET), Arrays.asList()),
					Arguments.of(Arrays.asList(JdbcObservationDocumentation.GENERATED_KEYS), Arrays.asList()),
					Arguments.of(Arrays.asList(), Arrays.asList())
			);
			// @formatter:on
		}

	}

	@Nested
	class Select extends Base {

		@BeforeEach
		void prepareData() throws Exception {
			// Base class clears data
			executeQuery(dataSource, "INSERT INTO emp (id, name) VALUES (20, 'Bar')");
		}

		@ParameterizedTest
		@ArgumentsSource(SelectDataProvider.class)
		void supportedTypes(List<JdbcObservationDocumentation> supportedTypeLists,
				List<JdbcObservationDocumentation> expectedTypes) throws Exception {
			DataSource proxyDataSource = createProxyDataSource(supportedTypeLists, this.registry);
			String result = doLogic(proxyDataSource);
			assertThat(result).isEqualTo("Bar");

			verifySpanNames(expectedTypes);
			// make sure, observation scope is closed.
			assertThat(this.registry.getCurrentObservation()).isNull();
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

	}

	static class SelectDataProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
			// @formatter:off
			return Stream.of(
					// input list, expected list
					Arguments.of(Arrays.asList(JdbcObservationDocumentation.values()),
							Arrays.asList(JdbcObservationDocumentation.CONNECTION, JdbcObservationDocumentation.QUERY, JdbcObservationDocumentation.RESULT_SET)),
					Arguments.of(Arrays.asList(JdbcObservationDocumentation.CONNECTION), Arrays.asList(JdbcObservationDocumentation.CONNECTION)),
					Arguments.of(Arrays.asList(JdbcObservationDocumentation.QUERY), Arrays.asList(JdbcObservationDocumentation.QUERY)),
					Arguments.of(Arrays.asList(JdbcObservationDocumentation.RESULT_SET), Arrays.asList(JdbcObservationDocumentation.RESULT_SET)),
					Arguments.of(Arrays.asList(JdbcObservationDocumentation.GENERATED_KEYS), Arrays.asList()),
					Arguments.of(Arrays.asList(), Arrays.asList())
			);
			// @formatter:on
		}

	}

}
