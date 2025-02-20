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

package net.ttddyy.observation.boot.autoconfigure;

import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import brave.test.TestSpanHandler;
import io.micrometer.tracing.brave.bridge.BraveFinishedSpan;
import io.micrometer.tracing.exporter.FinishedSpan;
import io.micrometer.tracing.test.simple.SpanAssert;
import io.micrometer.tracing.test.simple.SpansAssert;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.observation.tracing.DataSourceObservationListener;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the {@link DataSourceObservationAutoConfiguration}.
 *
 * @author Tadaya Tsuyukubo
 */
class DataSourceObservationAutoConfigurationIntegrationTests {

	// Each test defines own @SpringBootTest
	@AutoConfigureObservability
	static class TestCaseBase {

		@Autowired
		DataSource dataSource;

		@Autowired
		MyService myService;

		@Autowired
		TestSpanHandler testSpanHandler;

		@Autowired
		DataSourceObservationListener observationListener;

		private List<FinishedSpan> getFinishedSpans() {
			return this.testSpanHandler.spans().stream().map(BraveFinishedSpan::fromBrave).collect(Collectors.toList());
		}

		@Test
		void bootIntegration() {
			// verify basic things
			assertThat(this.dataSource).isInstanceOfSatisfying(ProxyDataSource.class, (ds) -> {
				// check datasource-proxy listener is added
				assertThat(ds.getProxyConfig().getMethodListener().getListeners()).contains(this.observationListener);
				assertThat(ds.getProxyConfig().getQueryListener().getListeners()).contains(this.observationListener);
			});

			// verify initial table/data creation
			SpansAssert.assertThat(getFinishedSpans())
				.hasNumberOfSpansWithNameEqualTo("query", 3)
				.assertThatASpanWithNameEqualTo("query")
				.hasTagWithKey("jdbc.query[0]");

			List<FinishedSpan> querySpans = getFinishedSpans().stream()
				.filter((span) -> "query".equals(span.getName()))
				.toList();
			SpanAssert.assertThat(querySpans.get(0))
				.hasTag("jdbc.query[0]", "CREATE TABLE emp(id INT, name VARCHAR(20))");
			SpansAssert.assertThat(querySpans.subList(1, 3)).allSatisfy((span) -> {
				SpanAssert.assertThat(span).hasTagWithKey("jdbc.query[0]");
			});
			this.testSpanHandler.clear();

			// perform business operation
			this.myService.add(100, "FOO");

			// verify the add operation
			SpansAssert.assertThat(getFinishedSpans())
				.hasASpanWithName("query",
						(spanAssert) -> spanAssert.hasTag("jdbc.query[0]", "INSERT INTO emp VALUES (?, ?)")
							.doesNotHaveTagWithKey("jdbc.param[0]"))
				.hasASpanWithName("connection", (spanAssert -> spanAssert.hasEventWithNameEqualTo("commit")));
			this.testSpanHandler.clear();

			// perform count and verify
			int count = this.myService.count();
			assertThat(count).isEqualTo(3);
			SpansAssert.assertThat(getFinishedSpans())
				.hasASpanWithName("query",
						(spanAssert) -> spanAssert.hasTag("jdbc.query[0]", "SELECT COUNT(*) FROM emp"))
				.hasASpanWithName("result-set", (spanAssert) -> spanAssert.hasTag("jdbc.row-count", "1"))
				.hasASpanWithName("connection");
		}

	}

	@Nested
	@SpringBootTest(
	// @formatter:off
			properties = {
					// embedded DB
					"spring.datasource.url=jdbc:h2:mem:testdb",
					"spring.datasource.driverClassName=org.h2.Driver",
					"spring.datasource.username=sa",

					// populate db
					"spring.sql.init.schema-locations=classpath:itest-schema.sql",
					"spring.sql.init.data-locations=classpath:itest-data.sql",

					// tracing
					"management.tracing.sampling.probability=1.0",

					// specify query logging
					"jdbc.datasource-proxy.logging=slf4j",
					"jdbc.datasource-proxy.query.enable-logging=true",
					"jdbc.datasource-proxy.query.log-level=DEBUG",
					"jdbc.datasource-proxy.query.logger-name=my.query-logger",
					"logging.level.my.query-logger=DEBUG",

					// for debugging, log spans
					"logging.level.brave.Tracer=INFO"
			},
	// @formatter:on
			args = "--debug")
	class WithAutoConfigurationDataSource extends TestCaseBase {

	}

	@Nested
	@SpringBootTest(
	// @formatter:off
			properties = {
					"spring.sql.init.schema-locations=classpath:itest-schema.sql",
					"spring.sql.init.data-locations=classpath:itest-data.sql",

					// tracing
					"management.tracing.sampling.probability=1.0",

					// specify query logging
					"jdbc.datasource-proxy.logging=slf4j",
					"jdbc.datasource-proxy.query.enable-logging=true",
					"jdbc.datasource-proxy.query.log-level=DEBUG",
					"jdbc.datasource-proxy.query.logger-name=my.query-logger",
					"logging.level.my.query-logger=DEBUG",

					// for debugging, log spans
					"logging.level.brave.Tracer=INFO"
			},
			// @formatter:on
			args = "--debug", classes = { MyDataSourceConfiguration.class })
	class WithManualDataSourceBean extends TestCaseBase {

	}

	@TestConfiguration(proxyBeanMethods = false)
	static class MyDataSourceConfiguration {

		// Define DataSource bean
		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).generateUniqueName(true).build();
		}

	}

	@SpringBootApplication
	static class MyApplication {

		@Bean
		public MyService myService(DataSource dataSource) {
			return new MyService(dataSource);
		}

		@Bean
		public TestSpanHandler spanHandler() {
			return new TestSpanHandler();
		}

	}

	@Transactional
	static class MyService {

		private final JdbcTemplate jdbcTemplate;

		public MyService(DataSource dataSource) {
			this.jdbcTemplate = new JdbcTemplate(dataSource);
		}

		public void add(int id, String name) {
			this.jdbcTemplate.update("INSERT INTO emp VALUES (?, ?)", id, name);
		}

		public int count() {
			Integer result = this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM emp", Integer.class);
			return (result != null ? result : 0);
		}

	}

}
