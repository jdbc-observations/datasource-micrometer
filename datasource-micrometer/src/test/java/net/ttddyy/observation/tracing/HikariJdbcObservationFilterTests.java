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

import java.sql.SQLException;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.tck.ObservationContextAssert;
import net.ttddyy.observation.tracing.JdbcObservationDocumentation.ConnectionKeyNames;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HikariJdbcObservationFilter}.
 *
 * @author Tadaya Tsuyukubo
 */
class HikariJdbcObservationFilterTests {

	private static final String DRIVER = "my-driver";

	private static final String POOL = "my-pool";

	@Test
	void filter() throws Exception {
		DataSource dataSource = givenHikariDataSource();

		ConnectionContext context = new ConnectionContext();
		context.setDataSource(dataSource);

		HikariJdbcObservationFilter filter = new HikariJdbcObservationFilter();
		Context result = filter.map(context);

		assertObservationHasHikariKeys(result);
	}

	@Test
	void nonHikariDataSource() throws Exception {
		DataSource dataSource = mock(DataSource.class);
		given(dataSource.isWrapperFor(HikariDataSource.class)).willReturn(false);

		ConnectionContext context = new ConnectionContext();
		context.setDataSource(dataSource);

		HikariJdbcObservationFilter filter = new HikariJdbcObservationFilter();
		Context result = filter.map(context);

		assertNotApplicableContext(context, result);
	}

	@Test
	void unwrapFailure() throws Exception {
		DataSource dataSource = mock(DataSource.class);
		given(dataSource.isWrapperFor(HikariDataSource.class)).willReturn(true);
		given(dataSource.unwrap(HikariDataSource.class)).willThrow(SQLException.class);

		ConnectionContext context = new ConnectionContext();
		context.setDataSource(dataSource);

		HikariJdbcObservationFilter filter = new HikariJdbcObservationFilter();
		Context result = filter.map(context);

		assertNotApplicableContext(context, result);
	}

	@Test
	void queryContext() throws Exception {
		DataSource dataSource = givenHikariDataSource();

		QueryContext context = new QueryContext();
		context.setDataSource(dataSource);

		HikariJdbcObservationFilter filter = new HikariJdbcObservationFilter();
		Context result = filter.map(context);

		assertObservationHasHikariKeys(result);
	}

	@Test
	void resultSetContext() throws Exception {
		DataSource dataSource = givenHikariDataSource();

		ResultSetContext context = new ResultSetContext();
		context.setDataSource(dataSource);

		HikariJdbcObservationFilter filter = new HikariJdbcObservationFilter();
		Context result = filter.map(context);

		assertObservationHasHikariKeys(result);
	}

	private static void assertObservationHasHikariKeys(Context result) {
		ObservationContextAssert.assertThat(result)
			.hasLowCardinalityKeyValue(ConnectionKeyNames.DATASOURCE_DRIVER.asString(), DRIVER)
			.hasLowCardinalityKeyValue(ConnectionKeyNames.DATASOURCE_POOL.asString(), POOL);
	}

	private static DataSource givenHikariDataSource() throws SQLException {
		HikariDataSource hikariDataSource = mock(HikariDataSource.class);
		given(hikariDataSource.getDriverClassName()).willReturn(DRIVER);
		given(hikariDataSource.getPoolName()).willReturn(POOL);
		DataSource dataSource = mock(DataSource.class);
		given(dataSource.isWrapperFor(HikariDataSource.class)).willReturn(true);
		given(dataSource.unwrap(HikariDataSource.class)).willReturn(hikariDataSource);
		return dataSource;
	}

	private void assertNotApplicableContext(Context original, Context result) {
		assertThat(result).isSameAs(original);
		ObservationContextAssert.assertThat(result)
			.doesNotHaveLowCardinalityKeyValueWithKey(ConnectionKeyNames.DATASOURCE_DRIVER.name())
			.doesNotHaveLowCardinalityKeyValueWithKey(ConnectionKeyNames.DATASOURCE_POOL.name());
	}

}
