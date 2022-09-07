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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Deque;
import java.util.function.BiConsumer;

import javax.sql.DataSource;

import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.test.SampleTestRunner;
import io.micrometer.tracing.test.reporter.BuildingBlocks;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for {@link DataSourceObservationListener} integration tests.
 *
 * @author Tadaya Tsuyukubo
 */
abstract class DataSourceListenerIntegrationTestBase extends SampleTestRunner {

	protected static JdbcDataSource dataSource;

	protected DataSource proxyDataSource;

	public DataSourceListenerIntegrationTestBase() {
		super(SampleRunnerConfig.builder().build());
	}

	@BeforeAll
	static void setUpDataSource() throws Exception {
		dataSource = createDataSource();
		executeQuery(dataSource, "CREATE TABLE emp(id INT, name VARCHAR(20))");
	}

	private static JdbcDataSource createDataSource() {
		JdbcDataSource ds = new JdbcDataSource();
		ds.setURL("jdbc:h2:mem:mytest;DB_CLOSE_DELAY=-1");
		ds.setUser("sa");
		ds.setPassword("sa");
		return ds;
	}

	protected static void executeQuery(DataSource ds, String query) throws Exception {
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

	@BeforeEach
	void createProxyDataSource() {
		DataSourceObservationListener listener = new DataSourceObservationListener(getObservationRegistry());
		customizeListener(listener);
		ProxyDataSourceBuilder builder = ProxyDataSourceBuilder.create(dataSource).listener(listener).name("proxy-ds")
				.methodListener(listener);
		customizeProxyDataSourceBuilder(builder);
		proxyDataSource = builder.build();
	}

	protected void customizeListener(DataSourceObservationListener listener) {

	}

	protected void customizeProxyDataSourceBuilder(ProxyDataSourceBuilder builder) {

	}

	@AfterEach
	void clearData() throws Exception {
		dataSource.getConnection().createStatement().execute("DELETE FROM emp");
	}

	protected int countTable() throws Exception {
		try (Connection conn = dataSource.getConnection()) {
			try (ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM emp")) {
				rs.next();
				return rs.getInt(1);
			}
		}
	}

	@Override
	public BiConsumer<BuildingBlocks, Deque<ObservationHandler<? extends Context>>> customizeObservationHandlers() {
		return (bb, handlers) -> {
			handlers.addFirst(new ConnectionTracingObservationHandler(bb.getTracer()));
			handlers.addFirst(new QueryTracingObservationHandler(bb.getTracer()));
			handlers.addFirst(new ResultSetTracingObservationHandler(bb.getTracer()));
		};
	}

}
