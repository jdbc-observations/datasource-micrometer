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

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.common.lang.Nullable;
import net.ttddyy.observation.tracing.QueryContext;
import net.ttddyy.observation.tracing.QueryObservationConvention;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Observation convention for OpenTelemetry Semantic Conventions.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.3.0
 */
public class OpenTelemetryQueryObservationConvention implements QueryObservationConvention {

	// TODO: move to documentation class
	public static final String DB_SYSTEM_NAME = "db.system.name";

	public static final String DB_COLLECTION_NAME = "db.collection.name";

	public static final String DB_NAMESPACE = "db.namespace";

	public static final String DB_OPERATION_NAME = "db.operation.name";

	public static final String DB_RESPONSE_STATUS_CODE = "db.response.status_code";

	public static final String ERROR_TYPE = "error.type";

	public static final String SERVER_PORT = "server.port";

	public static final String DB_OPERATION_BATCH_SIZE = "db.operation.batch.size";

	public static final String DB_QUERY_SUMMARY = "db.query.summary";

	public static final String DB_QUERY_TEXT = "db.query.text";

	public static final String DB_STORED_PROCEDURE_NAME = "db.stored_procedure.name";

	public static final String SERVER_ADDRESS = "server.address";

	private final OpenTelemetryAttributesManager attributesManager;

	private Map<String, String> attributesOverrides = new HashMap<>();

	private OpenTelemetrySpanNameBuilder spanNameBuilder = new OpenTelemetrySpanNameBuilder();

	public OpenTelemetryQueryObservationConvention(OpenTelemetryAttributesManager attributesManager) {
		this.attributesManager = attributesManager;
	}

	@Override
	public String getContextualName(QueryContext context) {
		OpenTelemetryAttributes attributes = this.attributesManager.getOrCreateAttributes(context);
		return this.spanNameBuilder.build(attributes);
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(QueryContext context) {
		OpenTelemetryAttributes attributes = this.attributesManager.getOrCreateAttributes(context);
		Set<KeyValue> keyValues = new HashSet<>();
		addKeyValueIfNotNull(keyValues, DB_SYSTEM_NAME, attributes.getSystemName());
		addKeyValueIfNotNull(keyValues, DB_COLLECTION_NAME, attributes.getCollectionName());
		addKeyValueIfNotNull(keyValues, DB_NAMESPACE, attributes.getNamespace());
		addKeyValueIfNotNull(keyValues, DB_OPERATION_NAME, attributes.getOperationName());
		addKeyValueIfNotNull(keyValues, SERVER_PORT, attributes.getServerPort());
		addKeyValueIfNotNull(keyValues, SERVER_ADDRESS, attributes.getServerAddress());
		return KeyValues.of(keyValues);
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(QueryContext context) {
		OpenTelemetryAttributes attributes = this.attributesManager.getOrCreateAttributes(context);
		Set<KeyValue> keyValues = new HashSet<>();
		addKeyValueIfNotNull(keyValues, DB_RESPONSE_STATUS_CODE, attributes.getResponseStatusCode());
		addKeyValueIfNotNull(keyValues, ERROR_TYPE, attributes.getErrorType());
		addKeyValueIfNotNull(keyValues, DB_OPERATION_BATCH_SIZE, attributes.getBatchSize());
		addKeyValueIfNotNull(keyValues, DB_QUERY_SUMMARY, attributes.getQuerySummary());
		addKeyValueIfNotNull(keyValues, DB_QUERY_TEXT, attributes.getQueryText());
		addKeyValueIfNotNull(keyValues, DB_STORED_PROCEDURE_NAME, attributes.getStoredProcedureName());
		return KeyValues.of(keyValues);
	}

	private void addKeyValueIfNotNull(Set<KeyValue> keyValues, String key, @Nullable String defaultValue) {
		String value = this.attributesOverrides.getOrDefault(key, defaultValue);
		if (value != null) {
			keyValues.add(KeyValue.of(key, value));
		}
	}

	public void setSpanNameBuilder(OpenTelemetrySpanNameBuilder spanNameBuilder) {
		this.spanNameBuilder = spanNameBuilder;
	}

	public void setAttributesOverrides(Map<String, String> attributesOverrides) {
		this.attributesOverrides = attributesOverrides;
	}
}
