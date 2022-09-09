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
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationFilter;
import net.ttddyy.observation.tracing.JdbcObservation.ConnectionKeyNames;

/**
 * A {@link ObservationFilter} to populate HikariCP specific information.
 * <p>
 * Analogous to the {@code TraceHikariListenerStrategySpanCustomizer} in Spring Cloud
 * Sleuth.
 *
 * @author Tadaya Tsuyukubo
 */
public class HikariJdbcObservationFilter implements ObservationFilter {

	@Override
	public Context map(Context context) {
		if (!(context instanceof ConnectionContext)) {
			return context;
		}

		ConnectionContext connectionContext = (ConnectionContext) context;
		DataSource dataSource = connectionContext.getDataSource();

		HikariDataSource hikariDataSource;
		try {
			// quick check before unwrap
			if (!dataSource.isWrapperFor(HikariDataSource.class)) {
				return context;
			}
			hikariDataSource = dataSource.unwrap(HikariDataSource.class);
		}
		catch (SQLException exception) {
			// ignore
			return context;
		}

		String driverClassName = hikariDataSource.getDriverClassName();
		if (StringUtils.isNotBlank(driverClassName)) {
			context.addLowCardinalityKeyValue(ConnectionKeyNames.DATASOURCE_DRIVER.withValue(driverClassName));
		}
		String poolName = hikariDataSource.getPoolName();
		if (StringUtils.isNotBlank(poolName)) {
			context.addLowCardinalityKeyValue(ConnectionKeyNames.DATASOURCE_POOL.withValue(poolName));
		}

		return context;
	}

}
