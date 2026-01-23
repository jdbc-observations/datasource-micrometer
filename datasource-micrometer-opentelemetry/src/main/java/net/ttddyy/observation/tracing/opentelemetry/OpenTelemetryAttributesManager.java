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

package net.ttddyy.observation.tracing.opentelemetry;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.StatementType;
import net.ttddyy.observation.tracing.QueryContext;

import java.sql.SQLException;

/**
 * Responsible for converting {@link OpenTelemetryAttributes} from {@link QueryContext}.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.3.0
 */
public class OpenTelemetryAttributesManager {

	private static final String CONTEXT_KEY = OpenTelemetryAttributes.class.getName();

	private final OpenTelemetryConnectionUrlParser connectionUrlParser;

	private final OpenTelemetryQueryAnalyzer queryAnalyzer;

	public OpenTelemetryAttributesManager(OpenTelemetryConnectionUrlParser connectionUrlParser,
			OpenTelemetryQueryAnalyzer queryAnalyzer) {
		this.connectionUrlParser = connectionUrlParser;
		this.queryAnalyzer = queryAnalyzer;
	}

	public OpenTelemetryAttributes getOrCreateAttributes(QueryContext context) {
		return context.computeIfAbsent(CONTEXT_KEY, (key) -> getAttributes(context));
	}

	public OpenTelemetryAttributes getAttributes(QueryContext queryContext) {
		ExecutionInfo executionInfo = queryContext.getExecutionInfo();
		UrlParseResult parseResult = parseConnectionUrl(queryContext);

		// in case for multiple queries??
		String query = queryContext.getQueryInfoList().get(0).getQuery();
		// Otel considers batch only when batchSize > 1;
		boolean isBatch = executionInfo.isBatch() && executionInfo.getBatchSize() > 1;
		StatementType statementType = executionInfo.getStatementType();
		QueryAnalysisResult queryAnalysisResult = analyzeQuery(query, isBatch, statementType);

		OpenTelemetryAttributes attributes = new OpenTelemetryAttributes();
		attributes.populateFromQueryContext(queryContext);
		attributes.populateFromUrlParseResult(parseResult);
		attributes.populateFromAnalysisResult(queryAnalysisResult);

		if (isBatch) {
			attributes.setBatchSize(Integer.toString(executionInfo.getBatchSize()));
		}

		Throwable error = executionInfo.getThrowable();
		if (error != null) {
			attributes.setErrorType(error.getClass().getName());
			if (error instanceof SQLException) {
				SQLException sqlException = (SQLException) error;
				attributes.setResponseStatusCode(Integer.toString(sqlException.getErrorCode()));
			}
		}

		return attributes;
	}

	protected UrlParseResult parseConnectionUrl(QueryContext context) {
		String url = context.getUrl();
		if (url != null) {
			return this.connectionUrlParser.parse(url);
		}
		return new UrlParseResult();
	}

	protected QueryAnalysisResult analyzeQuery(String query, boolean isBatch, StatementType statementType) {
		return this.queryAnalyzer.analyze(query, isBatch, statementType);
	}

}
