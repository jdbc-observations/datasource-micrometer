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

package net.ttddyy.observation.tracing;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import io.micrometer.tracing.exporter.FinishedSpan;
import io.micrometer.tracing.test.simple.SpanAssert;
import io.micrometer.tracing.test.simple.SpansAssert;

import static org.assertj.core.api.Assertions.fail;

/**
 * Integration test for {@link DataSourceObservationListener} with a JDBC error case.
 *
 * @author Tadaya Tsuyukubo
 */
class DataSourceListenerFailureTest extends DataSourceListenerIntegrationTestBase {

	@Override
	public SampleTestRunnerConsumer yourCode() throws Exception {
		return (bb, meterRegistry) -> {
			doLogic();

			// @formatter:off
			SpansAssert.assertThat(bb.getFinishedSpans())
					.hasNumberOfSpansEqualTo(2)
					.hasASpanWithName("connection", (spanAssert -> {
						spanAssert
								.hasEventWithNameEqualTo("acquired")
								.hasEventWithNameEqualTo("rollback");
					}))
					.hasASpanWithName("query");
			// @formatter:on

			FinishedSpan querySpan = bb.getFinishedSpans().stream().filter(span -> "query".equals(span.getName()))
					.findFirst().orElseThrow(IllegalStateException::new);

			SpanAssert.assertThat(querySpan).assertThatThrowable()
					.hasMessageContaining("SELECT * FROM not-existing-table");
		};
	}

	private void doLogic() throws Exception {
		try (Connection conn = this.proxyDataSource.getConnection()) {
			try (Statement stmt = conn.createStatement()) {
				try {
					stmt.executeQuery("SELECT * FROM not-existing-table");
					fail("query should fail");
				}
				catch (SQLException ex) {
					// no-op
				}
			}
			conn.rollback();
		}
	}

}
