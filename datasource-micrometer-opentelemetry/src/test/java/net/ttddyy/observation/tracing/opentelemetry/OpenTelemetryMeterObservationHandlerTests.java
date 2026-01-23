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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.ttddyy.observation.tracing.QueryContext;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.micrometer.core.tck.MeterRegistryAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OpenTelemetryMeterObservationHandler}.
 */
class OpenTelemetryMeterObservationHandlerTests {

	@Test
	public void metric() {
		OpenTelemetryAttributesManager manager = mock(OpenTelemetryAttributesManager.class);
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		OpenTelemetryMeterObservationHandler handler = new OpenTelemetryMeterObservationHandler(manager, registry);

		OpenTelemetryAttributes attributes = new OpenTelemetryAttributes();
		attributes.setSystemName("system-name");
		attributes.setCollectionName("collection-name");
		attributes.setNamespace("namespace");
		attributes.setOperationName("operation-name");
		attributes.setResponseStatusCode("response-status-code");
		attributes.setErrorType("error-type");
		attributes.setServerPort("server-port");
		attributes.setQuerySummary("query-summary");
		attributes.setQueryText("query-text");
		attributes.setStoredProcedureName("stored-procedure-name");
		attributes.setServerAddress("server-address");
		attributes.setServerPort("server-port");

		QueryContext queryContext = new QueryContext();
		queryContext.setHost("host");
		queryContext.setPort(1234);
		given(manager.getOrCreateAttributes(queryContext)).willReturn(attributes);

		handler.onStart(queryContext);
		handler.onStop(queryContext);

		KeyValues keyValues = KeyValues.of(
		// @formatter:off
				KeyValue.of(OpenTelemetryQueryObservationConvention.DB_SYSTEM_NAME, "system-name"),
				KeyValue.of(OpenTelemetryQueryObservationConvention.DB_COLLECTION_NAME, "collection-name"),
				KeyValue.of(OpenTelemetryQueryObservationConvention.DB_NAMESPACE, "namespace"),
				KeyValue.of(OpenTelemetryQueryObservationConvention.DB_OPERATION_NAME, "operation-name"),
				KeyValue.of(OpenTelemetryQueryObservationConvention.DB_RESPONSE_STATUS_CODE, "response-status-code"),
				KeyValue.of(OpenTelemetryQueryObservationConvention.ERROR_TYPE, "error-type"),
				KeyValue.of(OpenTelemetryQueryObservationConvention.DB_QUERY_SUMMARY, "query-summary"),
				KeyValue.of(OpenTelemetryQueryObservationConvention.DB_QUERY_TEXT, "query-text"),
				KeyValue.of(OpenTelemetryQueryObservationConvention.DB_STORED_PROCEDURE_NAME, "stored-procedure-name"),
				KeyValue.of(OpenTelemetryQueryObservationConvention.SERVER_ADDRESS, "server-address"),
				KeyValue.of(OpenTelemetryQueryObservationConvention.SERVER_PORT, "server-port"),
				KeyValue.of("network.peer.address", "host"),
				KeyValue.of("network.peer.port", "1234")
				// @formatter:on
		);

		assertThat(registry).hasTimerWithNameAndTags("db.client.operation.duration", keyValues);
	}

	@Test
	public void metricWithNoAttributes() {
		OpenTelemetryAttributesManager manager = mock(OpenTelemetryAttributesManager.class);
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		OpenTelemetryMeterObservationHandler handler = new OpenTelemetryMeterObservationHandler(manager, registry);

		// no values from attributes
		OpenTelemetryAttributes attributes = new OpenTelemetryAttributes();
		QueryContext queryContext = new QueryContext();
		given(manager.getOrCreateAttributes(queryContext)).willReturn(attributes);

		handler.onStart(queryContext);
		handler.onStop(queryContext);

		KeyValues keyValues = KeyValues.of(KeyValue.of("network.peer.port", "0"));

		assertThat(registry).hasTimerWithNameAndTags("db.client.operation.duration", keyValues);
	}

	@Test
	public void attributesOverride() {
		OpenTelemetryAttributesManager manager = mock(OpenTelemetryAttributesManager.class);
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		OpenTelemetryMeterObservationHandler handler = new OpenTelemetryMeterObservationHandler(manager, registry);

		OpenTelemetryAttributes attributes = new OpenTelemetryAttributes();
		attributes.setSystemName("system-name");
		attributes.setCollectionName("collection-name");
		attributes.setNamespace("namespace");
		attributes.setOperationName("operation-name");
		attributes.setResponseStatusCode("response-status-code");
		attributes.setErrorType("error-type");
		attributes.setServerPort("server-port");
		attributes.setQuerySummary("query-summary");
		attributes.setQueryText("query-text");
		attributes.setStoredProcedureName("stored-procedure-name");
		attributes.setServerAddress("server-address");
		attributes.setServerPort("server-port");

		QueryContext queryContext = new QueryContext();
		queryContext.setHost("host");
		queryContext.setPort(1234);
		given(manager.getOrCreateAttributes(queryContext)).willReturn(attributes);

		Map<String, String> override = new HashMap<>();
		override.put(OpenTelemetryQueryObservationConvention.DB_SYSTEM_NAME, "system-name-override");
		override.put(OpenTelemetryQueryObservationConvention.DB_COLLECTION_NAME, "collection-name-override");
		override.put(OpenTelemetryQueryObservationConvention.DB_NAMESPACE, "namespace-override");
		override.put(OpenTelemetryQueryObservationConvention.DB_OPERATION_NAME, "operation-name-override");
		override.put(OpenTelemetryQueryObservationConvention.DB_RESPONSE_STATUS_CODE, "response-status-code-override");
		override.put(OpenTelemetryQueryObservationConvention.ERROR_TYPE, "error-type-override");
		override.put(OpenTelemetryQueryObservationConvention.DB_QUERY_SUMMARY, "query-summary-override");
		override.put(OpenTelemetryQueryObservationConvention.DB_QUERY_TEXT, "query-text-override");
		override.put(OpenTelemetryQueryObservationConvention.DB_STORED_PROCEDURE_NAME,
				"stored-procedure-name-override");
		override.put(OpenTelemetryQueryObservationConvention.SERVER_ADDRESS, "server-address-override");
		override.put(OpenTelemetryQueryObservationConvention.SERVER_PORT, "server-port-override");
		override.put("network.peer.address", "host-override");
		override.put("network.peer.port", "1234-override");
		handler.setAttributesOverrides(override);

		handler.onStart(queryContext);
		handler.onStop(queryContext);

		KeyValues keyValues = KeyValues.of(
		// @formatter:off
				KeyValue.of(OpenTelemetryQueryObservationConvention.DB_SYSTEM_NAME, "system-name-override"),
				KeyValue.of(OpenTelemetryQueryObservationConvention.DB_COLLECTION_NAME, "collection-name-override"),
				KeyValue.of(OpenTelemetryQueryObservationConvention.DB_NAMESPACE, "namespace-override"),
				KeyValue.of(OpenTelemetryQueryObservationConvention.DB_OPERATION_NAME, "operation-name-override"),
				KeyValue.of(OpenTelemetryQueryObservationConvention.DB_RESPONSE_STATUS_CODE, "response-status-code-override"),
				KeyValue.of(OpenTelemetryQueryObservationConvention.ERROR_TYPE, "error-type-override"),
				KeyValue.of(OpenTelemetryQueryObservationConvention.DB_QUERY_SUMMARY, "query-summary-override"),
				KeyValue.of(OpenTelemetryQueryObservationConvention.DB_QUERY_TEXT, "query-text-override"),
				KeyValue.of(OpenTelemetryQueryObservationConvention.DB_STORED_PROCEDURE_NAME, "stored-procedure-name-override"),
				KeyValue.of(OpenTelemetryQueryObservationConvention.SERVER_ADDRESS, "server-address-override"),
				KeyValue.of(OpenTelemetryQueryObservationConvention.SERVER_PORT, "server-port-override"),
				KeyValue.of("network.peer.address", "host-override"),
				KeyValue.of("network.peer.port", "1234-override")
				// @formatter:on
		);

		assertThat(registry).hasTimerWithNameAndTags("db.client.operation.duration", keyValues);
	}

}
