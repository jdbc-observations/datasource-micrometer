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

package net.ttddyy.observation.boot.autoconfigure;

import java.sql.Connection;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link DefaultDataSourceNameResolver}
 *
 * @author Tadaya Tsuyukubo
 */
class DefaultDataSourceNameResolverTests {

	@Test
	void hikariPoolName() {
		DefaultDataSourceNameResolver resolver = new DefaultDataSourceNameResolver();
		HikariDataSource hikariDataSource = new HikariDataSource();
		hikariDataSource.setPoolName("foo-pool");

		String resolved = resolver.resolve("foo-bean", hikariDataSource);
		assertThat(resolved).isEqualTo("foo-pool");
	}

	@Test
	void resolvedFromCatalog() throws Exception {
		Connection connection = mock(Connection.class);
		DataSource dataSource = mock(DataSource.class);
		given(dataSource.getConnection()).willReturn(connection);
		given(connection.getCatalog()).willReturn("foo-catalog");

		DefaultDataSourceNameResolver resolver = new DefaultDataSourceNameResolver();
		String resolved = resolver.resolve("foo-bean", dataSource);
		assertThat(resolved).isEqualTo("foo-catalog");
	}

	@Test
	void resolvedDefault() throws Exception {
		Connection connection = mock(Connection.class);
		DataSource dataSource = mock(DataSource.class);
		given(dataSource.getConnection()).willReturn(connection);

		// make the catalog name empty
		given(connection.getCatalog()).willReturn(null);

		DefaultDataSourceNameResolver resolver = new DefaultDataSourceNameResolver();
		String resolved = resolver.resolve("foo-bean", dataSource);
		assertThat(resolved).isEqualTo("foo-bean");
	}

}
