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

import io.micrometer.observation.Observation;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static net.ttddyy.observation.tracing.RecordingObservationHandler.OperationType.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link DataSourceObservationListener} with data reference(select
 * query).
 *
 * @author Tadaya Tsuyukubo
 */
class DataSourceListenerOpenInViewIntegrationTest extends DataSourceListenerIntegrationTestBase {

	@BeforeEach
	void addData() throws Exception {
		executeQuery(dataSource, "INSERT INTO emp VALUES (10, 'Foo')");
		executeQuery(dataSource, "INSERT INTO emp VALUES (20, 'Bar')");
		executeQuery(dataSource, "INSERT INTO emp VALUES (30, 'Baz')");
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

			// verify observation interactions
			List<RecordingObservationHandler.ObservationOperation> operations = this.recordingObservationHandler.operations
				.stream()
				.filter(operation -> operation.context.getName().equals("my.observation")
						|| operation.context instanceof DataSourceBaseContext)
				.collect(Collectors.toList());
			assertThat(operations).satisfiesExactly(o -> o.verify(OB_START, "my.observation", null),
					o -> o.verify(SCOPE_OPENED, "my.observation", null),
					o -> o.verify(OB_START, "jdbc.connection", "my.observation"),
					o -> o.verify(SCOPE_OPENED, "jdbc.connection", "my.observation"),
					o -> o.verify(OB_EVENT, "jdbc.connection", "my.observation"),
					o -> o.verify(SCOPE_CLOSED, "jdbc.connection", "my.observation"),
					o -> o.verify(OB_START, "jdbc.query", "jdbc.connection"),
					o -> o.verify(SCOPE_OPENED, "jdbc.query", "jdbc.connection"),
					o -> o.verify(SCOPE_CLOSED, "jdbc.query", "jdbc.connection"),
					o -> o.verify(OB_STOP, "jdbc.query", "jdbc.connection"),
					o -> o.verify(OB_START, "jdbc.result-set", "jdbc.connection"),
					o -> o.verify(SCOPE_OPENED, "jdbc.result-set", "jdbc.connection"),
					o -> o.verify(SCOPE_CLOSED, "jdbc.result-set", "jdbc.connection"),
					o -> o.verify(OB_STOP, "jdbc.result-set", "jdbc.connection"),
					o -> o.verify(SCOPE_CLOSED, "my.observation", null), o -> o.verify(OB_STOP, "my.observation", null),
					o -> o.verify(OB_STOP, "jdbc.connection", "my.observation"));
		};
	}

	private String doLogic() throws Exception {
		// Simulate Open-In-View behavior: Close the DB connection after custom
		// observation is stopped.
		AtomicReference<Connection> connectionHolder = new AtomicReference<>();
		AtomicReference<String> resultHolder = new AtomicReference<>();

		Observation.createNotStarted("my.observation", getObservationRegistry()).observeChecked(() -> {
			Connection conn = this.proxyDataSource.getConnection();
			connectionHolder.set(conn);
			try (PreparedStatement stmt = conn.prepareStatement("SELECT name FROM emp WHERE id = ?")) {
				stmt.setInt(1, 20);
				try (ResultSet rs = stmt.executeQuery()) {
					rs.next();
					resultHolder.set(rs.getString(1));
				}
			}
		});

		// close the connection outside of custom observation
		connectionHolder.get().close();

		return resultHolder.get();
	}

}
