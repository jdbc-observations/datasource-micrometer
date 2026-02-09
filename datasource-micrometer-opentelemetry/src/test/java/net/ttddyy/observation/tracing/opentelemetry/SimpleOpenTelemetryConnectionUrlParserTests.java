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

package net.ttddyy.observation.tracing.opentelemetry;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SimpleOpenTelemetryConnectionUrlParser}.
 *
 * @author Tadaya Tsuyukubo
 */
class SimpleOpenTelemetryConnectionUrlParserTests {

	@ParameterizedTest
	@CsvSource({
	// @formatter:off
			"postgresql, jdbc:postgresql://localhost:5432/mydb",
			"postgresql, jdbc:postgresql://://remoteserver.com",
			"postgresql, jdbc:postgresql://localhost:5432/mydb?ssl=true",
			"postgresql, jdbc:postgresql://localhost:5432/mydb?currentSchema=myschema",
			"mysql, jdbc:mysql://myhost1:3306,myhost2:3307/db_name",
			"mysql, jdbc:mysql://[myhost1:3306,myhost2:3307]/db_name",
			"mysql, jdbc:mysql:loadbalance://myhost1:3306,myhost2:3307/db_name?user=dbUser&password=1234567&loadBalanceConnectionGroup=group_name&ha.enableJMX=true",
			"mariadb, jdbc:mariadb://localhost:3306/",
			"mariadb, jdbc:mariadb://localhost:3306/myDatabase?user=root&password=myPassword",
			"mariadb, jdbc:mariadb://server1:3306,server2:3306/mydb?failover=true",
			"microsoft.sql_server, jdbc:sqlserver://localhost:1433;databaseName=mydatabase",
			"microsoft.sql_server, jdbc:sqlserver://192.168.1.1:1433;databaseName=myDB;user=admin;password=pass",
			"microsoft.sql_server, jdbc:sqlserver://localhost:1433;databaseName=myDB;integratedSecurity=true;",
			"microsoft.sql_server, jdbc:sqlserver://192.168.1.1\\myInstance;databaseName=myDB",
			"microsoft.sql_server, jdbc:sqlserver://192.168.1.1\\myInstance;databaseName=myDB",
			"microsoft.sql_server, jdbc:sqlserver://192.168.1.1:1433;databaseName=myDB;encrypt=true;trustServerCertificate=true;",
			"other_sql, jdbc:oracle:thin:@//localhost:1521/ORCL",
			"other_sql, jdbc:derby:testdb;create=true",
			"other_sql, jdbc:h2:mem:",
			"other_sql, jdbc:h2:~/test",
	// @formatter:on
	})
	void systemName(String expected, String url) {
		SimpleOpenTelemetryConnectionUrlParser parser = new SimpleOpenTelemetryConnectionUrlParser(
				Collections.emptyMap());
		UrlParseResult result = parser.parse(url);
		assertThat(result.getSystemName()).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({
	// @formatter:off
			"postgresql, jdbc:postgresql://localhost:5432/mydb",
			"mysql, jdbc:mysql://myhost1:3306,myhost2:3307/db_name",
			"mariadb, jdbc:mariadb://localhost:3306/",
			"microsoft.sql_server, jdbc:sqlserver://localhost:1433;databaseName=mydatabase"
	// @formatter:on
	})
	void namespaceRetrievers(String expected, String url) {
		Map<String, DatabaseNamespaceRetriever> retrievers = mock(Map.class);
		SimpleOpenTelemetryConnectionUrlParser parser = new SimpleOpenTelemetryConnectionUrlParser(retrievers);
		UrlParseResult result = parser.parse(url);
		verify(retrievers).get(expected);
		assertThat(result.getNamespace()).isNull();
	}

}
