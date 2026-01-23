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

import io.micrometer.common.lang.Nullable;

/**
 * Result from {@link OpenTelemetryQueryAnalyzer}.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.3.0
 */
public class QueryAnalysisResult {

	@Nullable
	private String collectionName;

	@Nullable
	private String storedProcedureName;

	@Nullable
	private String operationName;

	@Nullable
	private String querySummary;

	@Nullable
	private String queryText;

	@Nullable
	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(@Nullable String collectionName) {
		this.collectionName = collectionName;
	}

	@Nullable
	public String getStoredProcedureName() {
		return storedProcedureName;
	}

	public void setStoredProcedureName(@Nullable String storedProcedureName) {
		this.storedProcedureName = storedProcedureName;
	}

	@Nullable
	public String getOperationName() {
		return operationName;
	}

	public void setOperationName(String operationName) {
		this.operationName = operationName;
	}

	@Nullable
	public String getQuerySummary() {
		return querySummary;
	}

	public void setQuerySummary(@Nullable String querySummary) {
		this.querySummary = querySummary;
	}

	@Nullable
	public String getQueryText() {
		return queryText;
	}

	public void setQueryText(String queryText) {
		this.queryText = queryText;
	}

}
