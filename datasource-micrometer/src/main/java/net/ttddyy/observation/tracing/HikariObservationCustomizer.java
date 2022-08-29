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
import io.micrometer.common.util.StringUtils;
import io.micrometer.observation.Observation;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.observation.tracing.JdbcObservation.ConnectionKeyNames;

/**
 * A {@link ObservationCustomizer} for HikariCP.
 * <p>
 * Analogous to the {@code TraceHikariListenerStrategySpanCustomizer} in Spring Cloud
 * Sleuth.
 *
 * @author Tadaya Tsuyukubo
 */
public class HikariObservationCustomizer implements ObservationCustomizer {

	@Override
	public void customize(DataSource dataSource, Observation observation) {
		// apply only to the connection observation
		if (!(observation.getContext() instanceof ConnectionContext)) {
			return;
		}

		HikariDataSource hikariDataSource;
		try {
			hikariDataSource = dataSource.unwrap(HikariDataSource.class);
		}
		catch (SQLException ex) {
			// ignore
			return;
		}

		String driverClassName = hikariDataSource.getDriverClassName();
		if (StringUtils.isNotBlank(driverClassName)) {
			observation.lowCardinalityKeyValue(ConnectionKeyNames.DATASOURCE_DRIVER.withValue(driverClassName));
		}
		String poolName = hikariDataSource.getPoolName();
		if (StringUtils.isNotBlank(poolName)) {
			observation.lowCardinalityKeyValue(ConnectionKeyNames.DATASOURCE_POOL.withValue(poolName));
		}
	}

	@Override
	public boolean support(DataSource dataSource) {
		try {
			return dataSource.isWrapperFor(HikariDataSource.class);
		}
		catch (SQLException ex) {
			return false;
		}
	}

}
