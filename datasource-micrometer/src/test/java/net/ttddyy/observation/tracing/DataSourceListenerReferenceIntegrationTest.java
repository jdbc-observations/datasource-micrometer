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

package net.ttddyy.observation.tracing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import io.micrometer.tracing.test.simple.SpansAssert;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link DataSourceObservationListener} with data reference(select
 * query).
 *
 * @author Tadaya Tsuyukubo
 */
class DataSourceListenerReferenceIntegrationTest extends DataSourceListenerIntegrationTestBase {

	@BeforeEach
	void addData() throws Exception {
		executeQuery(dataSource, "INSERT INTO emp VALUES (10, 'Foo')");
		executeQuery(dataSource, "INSERT INTO emp VALUES (20, 'Bar')");
		executeQuery(dataSource, "INSERT INTO emp VALUES (30, 'Baz')");
	}

	@Override
	protected void customizeListener(DataSourceObservationListener listener) {
		listener.setIncludeParameterValues(true); // enable "jdbc.params[]" tag in query
	}

	@Override
	protected void customizeProxyDataSourceBuilder(ProxyDataSourceBuilder builder) {
		builder.proxyResultSet(); // enable ResultSet observation
		builder.name("proxy"); // translates to the service name
	}

	@Override
	public SampleTestRunnerConsumer yourCode() throws Exception {
		return (bb, meterRegistry) -> {
			String result = doLogic();
			assertThat(result).isEqualTo("Bar");

			// @formatter:off
			SpansAssert.assertThat(bb.getFinishedSpans())
					.hasASpanWithRemoteServiceName("proxy")
					.hasNumberOfSpansEqualTo(3)
					.hasASpanWithName("connection")
					.hasASpanWithName("query", (spanAssert -> {
						spanAssert
								.hasTag("jdbc.query[0]", "SELECT name FROM emp WHERE id = ?")
								.hasTag("jdbc.params[0]", "(20)")
								.hasTag("jdbc.datasource.name", "proxy");
					}))
					.hasASpanWithName("result-set", (spanAssert) -> {
						spanAssert
								.hasTag("jdbc.row-count", "1")
								.hasTag("jdbc.datasource.name", "proxy");
					});
			// @formatter:on
		};
	}

	private String doLogic() throws Exception {
		try (Connection conn = this.proxyDataSource.getConnection()) {
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
