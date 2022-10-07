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

	@Test
	void filter() throws Exception {
		HikariDataSource hikariDataSource = mock(HikariDataSource.class);
		given(hikariDataSource.getDriverClassName()).willReturn("my-driver");
		given(hikariDataSource.getPoolName()).willReturn("my-pool");
		DataSource dataSource = mock(DataSource.class);
		given(dataSource.isWrapperFor(HikariDataSource.class)).willReturn(true);
		given(dataSource.unwrap(HikariDataSource.class)).willReturn(hikariDataSource);

		ConnectionContext context = new ConnectionContext();
		context.setDataSource(dataSource);

		HikariJdbcObservationFilter filter = new HikariJdbcObservationFilter();
		Context result = filter.map(context);

		ObservationContextAssert.assertThat(result)
				.hasLowCardinalityKeyValue(ConnectionKeyNames.DATASOURCE_DRIVER.asString(), "my-driver")
				.hasLowCardinalityKeyValue(ConnectionKeyNames.DATASOURCE_POOL.asString(), "my-pool");
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
	void nonConnectionContext() {
		QueryContext context = new QueryContext();
		HikariJdbcObservationFilter filter = new HikariJdbcObservationFilter();
		Context result = filter.map(context);

		assertNotApplicableContext(context, result);
	}

	private void assertNotApplicableContext(Context original, Context result) {
		assertThat(result).isSameAs(original);
		ObservationContextAssert.assertThat(result)
				.doesNotHaveLowCardinalityKeyValueWithKey(ConnectionKeyNames.DATASOURCE_DRIVER.name())
				.doesNotHaveLowCardinalityKeyValueWithKey(ConnectionKeyNames.DATASOURCE_POOL.name());
	}

}
