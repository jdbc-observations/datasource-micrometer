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
import java.sql.PreparedStatement;

import io.micrometer.tracing.test.simple.SpansAssert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link DataSourceObservationListener} with data population(insert
 * query).
 *
 * @author Tadaya Tsuyukubo
 */
class DataSourceListenerUpdateIntegrationTest extends DataSourceListenerIntegrationTestBase {

	@Override
	public SampleTestRunnerConsumer yourCode() throws Exception {
		return (bb, meterRegistry) -> {
			doLogic();

			int count = countTable();
			assertThat(count).as("number of inserted record").isEqualTo(2);

			// @formatter:off
			SpansAssert.assertThat(bb.getFinishedSpans())
					.hasNumberOfSpansEqualTo(2)
					.hasASpanWithName("connection", (spanAssert -> {
						spanAssert
								.hasEventWithNameEqualTo("acquired")
								.hasEventWithNameEqualTo("commit");
					}))
					.hasASpanWithName("query", (spanAssert -> {
						spanAssert
								.hasTag("jdbc.query[0]", "INSERT INTO emp VALUES (?, ?)")
								.doesNotHaveTagWithKey("jdbc.params[0]");   // default is off
					}));
			// @formatter:on
		};
	}

	private void doLogic() throws Exception {
		try (Connection conn = this.proxyDataSource.getConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO emp VALUES (?, ?)")) {
				stmt.setInt(1, 100);
				stmt.setString(2, "FOO");
				stmt.addBatch();

				stmt.setInt(1, 200);
				stmt.setString(2, "BAR");
				stmt.addBatch();

				stmt.executeBatch();
			}
			conn.commit();
		}
	}

}
