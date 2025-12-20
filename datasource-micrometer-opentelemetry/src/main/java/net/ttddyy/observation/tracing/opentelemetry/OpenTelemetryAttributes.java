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
import net.ttddyy.observation.tracing.QueryContext;

/**
 * Holds all data required by OpenTelemetry Semantic Conventions support.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.3.0
 */
public class OpenTelemetryAttributes {

	private String systemName;

	@Nullable
	private String collectionName;

	@Nullable
	private String namespace;

	@Nullable
	private String operationName;

	private String serverPort;

	private String serverAddress;

	private String batchSize;

	@Nullable
	private String querySummary;

	@Nullable
	private String queryText;

	@Nullable
	private String storedProcedureName;

	private String responseStatusCode;

	private String errorType;

	public void populateFromUrlParseResult(UrlParseResult parseResult) {
		this.systemName = parseResult.getSystemName();
		this.namespace = parseResult.getDatabaseName();
	}

	public void populateFromQueryContext(QueryContext queryContext) {
		this.serverAddress = queryContext.getHost();
		this.serverPort = Integer.toString(queryContext.getPort());
	}

	public void populateFromAnalysisResult(QueryAnalysisResult queryAnalysisResult) {
		this.queryText = queryAnalysisResult.getQueryText();
		this.querySummary = queryAnalysisResult.getQuerySummary();
		this.collectionName = queryAnalysisResult.getCollectionName();
		this.operationName = queryAnalysisResult.getOperationName();
		this.storedProcedureName = queryAnalysisResult.getStoredProcedureName();
	}

	public String getSystemName() {
		return this.systemName;
	}

	public void setSystemName(String systemName) {
		this.systemName = systemName;
	}

	@Nullable
	public String getCollectionName() {
		return this.collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	@Nullable
	public String getNamespace() {
		return this.namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	@Nullable
	public String getOperationName() {
		return this.operationName;
	}

	public void setOperationName(String operationName) {
		this.operationName = operationName;
	}

	public String getResponseStatusCode() {
		return this.responseStatusCode;
	}

	public void setResponseStatusCode(String responseStatusCode) {
		this.responseStatusCode = responseStatusCode;
	}

	public String getErrorType() {
		return this.errorType;
	}

	public void setErrorType(String errorType) {
		this.errorType = errorType;
	}

	public String getServerPort() {
		return this.serverPort;
	}

	public void setServerPort(String serverPort) {
		this.serverPort = serverPort;
	}

	public String getBatchSize() {
		return this.batchSize;
	}

	public void setBatchSize(String batchSize) {
		this.batchSize = batchSize;
	}

	@Nullable
	public String getQuerySummary() {
		return this.querySummary;
	}

	public void setQuerySummary(@Nullable String querySummary) {
		this.querySummary = querySummary;
	}

	@Nullable
	public String getQueryText() {
		return this.queryText;
	}

	public void setQueryText(@Nullable String queryText) {
		this.queryText = queryText;
	}

	@Nullable
	public String getStoredProcedureName() {
		return this.storedProcedureName;
	}

	public void setStoredProcedureName(String storedProcedureName) {
		this.storedProcedureName = storedProcedureName;
	}

	public String getServerAddress() {
		return this.serverAddress;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

}
