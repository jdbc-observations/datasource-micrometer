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

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.ObservationContextAssert;
import io.micrometer.observation.tck.TestObservationRegistry;
import net.ttddyy.observation.tracing.JdbcObservation.ConnectionKeyNames;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HikariJdbcObservationCustomizer}.
 *
 * @author Tadaya Tsuyukubo
 */
class HikariJdbcObservationCustomizerTests {

	@Test
	void support() throws Exception {
		HikariJdbcObservationCustomizer customizer = new HikariJdbcObservationCustomizer();
		DataSource dataSource = mock(DataSource.class);
		assertThat(customizer.support(dataSource)).isFalse();

		given(dataSource.isWrapperFor(HikariDataSource.class)).willReturn(true);
		assertThat(customizer.support(dataSource)).isTrue();
	}

	@Test
	void customize() throws Exception {
		HikariJdbcObservationCustomizer customizer = new HikariJdbcObservationCustomizer();
		HikariDataSource hikariDataSource = mock(HikariDataSource.class);
		given(hikariDataSource.getDriverClassName()).willReturn("my-driver");
		given(hikariDataSource.getPoolName()).willReturn("my-pool");
		DataSource dataSource = mock(DataSource.class);
		given(dataSource.unwrap(HikariDataSource.class)).willReturn(hikariDataSource);

		ObservationRegistry registry = TestObservationRegistry.create();
		ConnectionContext connectionContext = new ConnectionContext();

		Observation observation = Observation.createNotStarted("test", connectionContext, registry);
		ObservationContextAssert.assertThat(observation.getContext())
				.doesNotHaveLowCardinalityKeyValueWithKey(ConnectionKeyNames.DATASOURCE_DRIVER.name())
				.doesNotHaveLowCardinalityKeyValueWithKey(ConnectionKeyNames.DATASOURCE_POOL.name());
		customizer.customize(dataSource, observation);

		ObservationContextAssert.assertThat(observation.getContext())
				.hasLowCardinalityKeyValue(ConnectionKeyNames.DATASOURCE_DRIVER.asString(), "my-driver")
				.hasLowCardinalityKeyValue(ConnectionKeyNames.DATASOURCE_POOL.asString(), "my-pool");
	}

	@Test
	void customizeWithNonConnectionContext() {
		ObservationRegistry registry = TestObservationRegistry.create();
		QueryContext queryContext = new QueryContext();
		DataSource dataSource = mock(DataSource.class);
		Observation observation = Observation.createNotStarted("test", queryContext, registry);
		HikariJdbcObservationCustomizer customizer = new HikariJdbcObservationCustomizer();
		customizer.customize(dataSource, observation);

		ObservationContextAssert.assertThat(queryContext)
				.doesNotHaveLowCardinalityKeyValueWithKey(ConnectionKeyNames.DATASOURCE_DRIVER.name())
				.doesNotHaveLowCardinalityKeyValueWithKey(ConnectionKeyNames.DATASOURCE_POOL.name());

		ResultSetContext resultSetContext = new ResultSetContext();
		observation = Observation.createNotStarted("test", resultSetContext, registry);
		customizer.customize(dataSource, observation);

		ObservationContextAssert.assertThat(resultSetContext)
				.doesNotHaveLowCardinalityKeyValueWithKey(ConnectionKeyNames.DATASOURCE_DRIVER.name())
				.doesNotHaveLowCardinalityKeyValueWithKey(ConnectionKeyNames.DATASOURCE_POOL.name());
	}

}
