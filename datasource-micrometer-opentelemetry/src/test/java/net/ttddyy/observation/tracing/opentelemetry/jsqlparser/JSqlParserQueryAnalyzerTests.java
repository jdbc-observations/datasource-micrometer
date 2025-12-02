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

import net.ttddyy.dsproxy.StatementType;
import net.ttddyy.observation.tracing.opentelemetry.QueryAnalysisResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JSqlParserQueryAnalyzer}.
 *
 * @author Tadaya Tsuyukubo
 **/
class JSqlParserQueryAnalyzerTests {

	@Test
	void analyze() {
		String query = "SELECT * FROM table";

		JSqlParserQueryAnalyzer analyzer = new JSqlParserQueryAnalyzer();
		QueryAnalysisResult result = analyzer.analyze(query, false, StatementType.PREPARED);

		assertThat(result.getQuerySummary()).isEqualTo("SELECT table");
		assertThat(result.getCollectionName()).isEqualTo("table");
		assertThat(result.getQueryText()).isEqualTo(query);
		assertThat(result.getOperationName()).isEqualTo("SELECT");
		assertThat(result.getStoredProcedureName()).isNull();
	}

	@Test
	void batch() {
		String query = "SELECT * FROM table";

		JSqlParserQueryAnalyzer analyzer = new JSqlParserQueryAnalyzer();
		QueryAnalysisResult result = analyzer.analyze(query, true, StatementType.PREPARED);

		assertThat(result.getOperationName()).isEqualTo("BATCH SELECT");
	}

	@Test
	void callable() {
		String query = "{CALL foo()}";

		JSqlParserQueryAnalyzer analyzer = new JSqlParserQueryAnalyzer();
		QueryAnalysisResult result = analyzer.analyze(query, false, StatementType.CALLABLE);

		assertThat(result.getQuerySummary()).isEqualTo("CALL foo");
		assertThat(result.getCollectionName()).isNull();
		assertThat(result.getQueryText()).isNull();
		assertThat(result.getOperationName()).isEqualTo("CALL");
		assertThat(result.getStoredProcedureName()).isEqualTo("foo");
	}

	@Test
	void invalid() {
		String query = "wrong query";

		JSqlParserQueryAnalyzer analyzer = new JSqlParserQueryAnalyzer();
		QueryAnalysisResult result = analyzer.analyze(query, false, StatementType.PREPARED);

		assertThat(result.getQuerySummary()).isNull();
		assertThat(result.getCollectionName()).isNull();
		assertThat(result.getQueryText()).isEqualTo(query);
		assertThat(result.getOperationName()).isNull();
		assertThat(result.getStoredProcedureName()).isNull();
	}

}
