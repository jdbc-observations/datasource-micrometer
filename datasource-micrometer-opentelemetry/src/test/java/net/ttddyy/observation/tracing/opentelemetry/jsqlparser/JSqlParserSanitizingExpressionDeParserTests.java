/*
 * Copyright 2025 the original author or authors.
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

package net.ttddyy.observation.tracing.opentelemetry.jsqlparser;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Tests for {@link JSqlParserSanitizingExpressionDeParser}.
 *
 * @author Tadaya Tsuyukubo
 **/
class JSqlParserSanitizingExpressionDeParserTests {

	@ParameterizedTest
	@MethodSource
	void sanitize(String query, String expected) throws Exception {
		Statement statement = CCJSqlParserUtil.parse(query);

		StringBuilder sb = new StringBuilder();
		StatementDeParser deParser = new StatementDeParser(new JSqlParserSanitizingExpressionDeParser(),
				new SelectDeParser(), sb);
		statement.accept(deParser);

		assertThat(sb.toString()).isEqualTo(expected);
	}

	static Stream<Arguments> sanitize() {
		// many of data came from:
		// https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation-api-incubator/src/test/java/io/opentelemetry/instrumentation/api/incubator/semconv/db/SqlStatementSanitizerTest.java
		return Stream.of(
		//@formatter:off
			arguments("SELECT * FROM TABLE WHERE FIELD = 1234", "SELECT * FROM TABLE WHERE FIELD = ?"),
			arguments("SELECT * FROM TABLE WHERE FIELD >= -1234", "SELECT * FROM TABLE WHERE FIELD >= ?"),
			arguments("SELECT * FROM TABLE WHERE FIELD < -1234", "SELECT * FROM TABLE WHERE FIELD < ?"),
			arguments("SELECT * FROM TABLE WHERE FIELD < .1234", "SELECT * FROM TABLE WHERE FIELD < ?"),
			arguments("SELECT 1.2", "SELECT ?"),
			arguments("SELECT -1.2", "SELECT ?"),
			arguments("SELECT -1.2e-9", "SELECT ?"),
			arguments("SELECT 2E+9", "SELECT ?"),
			arguments("SELECT +0.2", "SELECT ?"),
			arguments("SELECT .2", "SELECT ?"),
			arguments("SELECT 7", "SELECT ?"),
			arguments("SELECT .7", "SELECT ?"),
			arguments("SELECT -7", "SELECT ?"),
			arguments("SELECT +7", "SELECT ?"),
			arguments("SELECT 0x0af764", "SELECT ?"),
			arguments("SELECT 0xdeadBEEF", "SELECT ?"),
			arguments("SELECT * FROM \"TABLE\"", "SELECT * FROM \"TABLE\""),

			// Not numbers but could be confused as such
			arguments("SELECT A + B", "SELECT A + B"),
			arguments("SELECT * FROM TABLE123", "SELECT * FROM TABLE123"),
			arguments("SELECT FIELD2 FROM TABLE_123 WHERE X <> 7", "SELECT FIELD2 FROM TABLE_123 WHERE X <> ?"),

			arguments("SELECT DEADBEEF", "SELECT DEADBEEF"),
			arguments("SELECT 123-45-6789", "SELECT ? - ? - ?"),  // different from otel
			arguments("SELECT 1/2/34", "SELECT ? / ? / ?"),  // different from otel

			// Basic ' strings
			arguments("SELECT * FROM TABLE WHERE FIELD = ''", "SELECT * FROM TABLE WHERE FIELD = ?"),
			arguments("SELECT * FROM TABLE WHERE FIELD = 'words and spaces'",
					"SELECT * FROM TABLE WHERE FIELD = ?"),
			arguments("SELECT * FROM TABLE WHERE FIELD = ' an escaped '' quote mark inside'",
					"SELECT * FROM TABLE WHERE FIELD = ?"),
			arguments("SELECT * FROM TABLE WHERE FIELD = '\\\\'", "SELECT * FROM TABLE WHERE FIELD = ?"),
			arguments("SELECT * FROM TABLE WHERE FIELD = '\"inside doubles\"'",
					"SELECT * FROM TABLE WHERE FIELD = ?"),
			arguments("SELECT * FROM TABLE WHERE FIELD = '\"$$$$\"'", "SELECT * FROM TABLE WHERE FIELD = ?"),
			arguments("SELECT * FROM TABLE WHERE FIELD = 'a single \" doublequote inside'",
					"SELECT * FROM TABLE WHERE FIELD = ?"),

			// Some databases allow using dollar-quoted strings
//			arguments("SELECT * FROM TABLE WHERE FIELD = $$$$", "SELECT * FROM TABLE WHERE FIELD = ?"),
//			arguments("SELECT * FROM TABLE WHERE FIELD = $$words and spaces$$",
//					"SELECT * FROM TABLE WHERE FIELD = ?"),
//			arguments("SELECT * FROM TABLE WHERE FIELD = $$quotes '\" inside$$",
//					"SELECT * FROM TABLE WHERE FIELD = ?"),
//			arguments("SELECT * FROM TABLE WHERE FIELD = $$\"''\"$$", "SELECT * FROM TABLE WHERE FIELD = ?"),
//			arguments("SELECT * FROM TABLE WHERE FIELD = $$\\\\$$", "SELECT * FROM TABLE WHERE FIELD = ?"),

			// PostgreSQL native parameter marker, we want to keep $1 instead of
			// replacing it with ?
			arguments("SELECT * FROM TABLE WHERE FIELD = $1", "SELECT * FROM TABLE WHERE FIELD = $1"),

			// Unicode, including a unicode identifier with a trailing number
			arguments("SELECT * FROM TABLEओ7 WHERE FIELD = 'ɣ'", "SELECT * FROM TABLEओ7 WHERE FIELD = ?"),

			// whitespace normalization
			arguments("SELECT    *    \t\r\nFROM  TABLE WHERE FIELD1 = 12344 AND FIELD2 = 5678",
					"SELECT * FROM TABLE WHERE FIELD1 = ? AND FIELD2 = ?")
			//@formatter:on
		);
	}

}
