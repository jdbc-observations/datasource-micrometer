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
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.tracing.test.simple.SpansAssert;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import net.ttddyy.observation.tracing.JdbcObservation.ConnectionKeyNames;
import org.junit.jupiter.api.AfterEach;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hikari integration tests.
 *
 * @author Tadaya Tsuyukubo
 */
class HikariDataSourceIntegrationTests extends DataSourceListenerIntegrationTestBase {

	private HikariDataSource hikariDataSource;

	@Override
	protected void customizeProxyDataSourceBuilder(ProxyDataSourceBuilder builder) {
		builder.proxyResultSet();
		// Override datasource with HikariDataSource
		this.hikariDataSource = new HikariDataSource();
		this.hikariDataSource.setDataSource(dataSource);
		this.hikariDataSource.setDriverClassName(org.h2.Driver.class.getName());
		builder.dataSource(this.hikariDataSource);
	}

	@Override
	protected void customizeListener(DataSourceObservationListener listener) {
		listener.getObservationCustomizers().add(new HikariObservationCustomizer());
	}

	@AfterEach
	void shutDownHikari() {
		this.hikariDataSource.close();
	}

	@Override
	public SampleTestRunnerConsumer yourCode() throws Exception {
		return (bb, meterRegistry) -> {
			Set<String> result = doLogic();

			assertThat(result).containsExactlyInAnyOrder("FOO", "BAR");

			// @formatter:off
			SpansAssert.assertThat(bb.getFinishedSpans())
					.hasNumberOfSpansEqualTo(4)
					.hasASpanWithName("connection", (spanAssert -> {
						spanAssert.hasTag(ConnectionKeyNames.DATASOURCE_DRIVER, "org.h2.Driver");
						spanAssert.hasTagWithKey(ConnectionKeyNames.DATASOURCE_POOL);
						spanAssert.hasEventWithNameEqualTo("rollback");
					}))
					.hasNumberOfSpansWithNameEqualTo("query", 2)
					.hasASpanWithName("result-set", (spanAssert) -> {
						spanAssert.hasTag("jdbc.row-count", "2");
					});
			// @formatter:on
		};
	}

	private Set<String> doLogic() throws Exception {
		Set<String> result = new HashSet<>();
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

			try (PreparedStatement stmt = conn.prepareStatement("SELECT name FROM emp")) {
				try (ResultSet rs = stmt.executeQuery()) {
					while (rs.next()) {
						result.add(rs.getString(1));
					}
				}
			}
			conn.rollback();
		}
		return result;
	}

}
