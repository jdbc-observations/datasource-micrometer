/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ttddyy.observation.tracing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultJdbcConnectionInfoExtractor}.
 *
 * @author Tadaya Tsuyukubo
 */
class DefaultJdbcConnectionInfoExtractorTests {

	DefaultJdbcConnectionInfoExtractor extractor;

	@BeforeEach
	void setUp() {
		this.extractor = new DefaultJdbcConnectionInfoExtractor();
	}

	@Test
	void parsingFailure() throws Exception {
		Connection connection;
		JdbcConnectionInfoExtractor.JdbcConnectionInfo result;
		SQLException sqlException = mock(SQLException.class);

		connection = mock(Connection.class);
		doThrow(sqlException).when(connection).getMetaData();

		result = this.extractor.extract(connection);
		assertThat(result).isSameAs(DefaultJdbcConnectionInfoExtractor.EMPTY_RESULT);

		connection = mock(Connection.class);
		DatabaseMetaData metaData = mock(DatabaseMetaData.class);
		doReturn(metaData).when(connection).getMetaData();
		doThrow(sqlException).when(metaData).getURL();

		result = this.extractor.extract(connection);
		assertThat(result).isSameAs(DefaultJdbcConnectionInfoExtractor.EMPTY_RESULT);
	}

	@ParameterizedTest
	@ValueSource(strings = "localhost:{12345}") // "{","}" are not compliant to RFC 2396
	@NullSource
	void invalidUrl(String url) throws Exception {
		// valid, invalid
		Connection connection = mock(Connection.class);
		DatabaseMetaData metaData = mock(DatabaseMetaData.class);
		doReturn(metaData).when(connection).getMetaData();
		doReturn(url).when(metaData).getURL();

		JdbcConnectionInfoExtractor.JdbcConnectionInfo result = this.extractor.extract(connection);
		assertThat(result).isSameAs(DefaultJdbcConnectionInfoExtractor.EMPTY_RESULT);
	}

	@Test
	void extract() throws Exception {
		Connection connection = mock(Connection.class);
		DatabaseMetaData metaData = mock(DatabaseMetaData.class);
		doReturn(metaData).when(connection).getMetaData();
		doReturn("jdbc://localhost:5432").when(metaData).getURL();

		JdbcConnectionInfoExtractor.JdbcConnectionInfo result = this.extractor.extract(connection);

		assertThat(result.getUrl()).isEqualTo("jdbc://localhost:5432");
		assertThat(result.getHost()).isEqualTo("localhost");
		assertThat(result.getPort()).isEqualTo(5432);
	}

	@Test
	void cache() throws Exception {
		Connection connection = mock(Connection.class);
		DatabaseMetaData metaData = mock(DatabaseMetaData.class);
		doReturn(metaData).when(connection).getMetaData();
		doReturn("jdbc://localhost:5432", "jdbc://localhost:5432", "jdbc://remote:5432").when(metaData).getURL();

		JdbcConnectionInfoExtractor.JdbcConnectionInfo first = this.extractor.extract(connection);
		JdbcConnectionInfoExtractor.JdbcConnectionInfo second = this.extractor.extract(connection);
		JdbcConnectionInfoExtractor.JdbcConnectionInfo third = this.extractor.extract(connection);

		assertThat(first).isSameAs(second);
		assertThat(first).isNotSameAs(third);
	}

	@Test
	void clear() throws Exception {
		Connection connection = mock(Connection.class);
		DatabaseMetaData metaData = mock(DatabaseMetaData.class);
		doReturn(metaData).when(connection).getMetaData();
		doReturn("jdbc://localhost:5432", "jdbc://localhost:5432").when(metaData).getURL();

		JdbcConnectionInfoExtractor.JdbcConnectionInfo first = this.extractor.extract(connection);
		this.extractor.clear();
		JdbcConnectionInfoExtractor.JdbcConnectionInfo second = this.extractor.extract(connection);

		assertThat(first).isNotSameAs(second);
	}

}
