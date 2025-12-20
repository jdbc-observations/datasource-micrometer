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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import net.ttddyy.observation.tracing.QueryContext;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * OpenTelemetry Semantic Conventions metrics support.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.3.0
 */
public class OpenTelemetryMeterObservationHandler implements ObservationHandler<QueryContext> {

	private static final String DURATION = "db.client.operation.duration";

	private final OpenTelemetryAttributesManager attributesManager;

	private final MeterRegistry meterRegistry;

	private Map<String, String> attributesOverrides = new HashMap<>();

	public OpenTelemetryMeterObservationHandler(OpenTelemetryAttributesManager attributesManager,
			MeterRegistry meterRegistry) {
		this.attributesManager = attributesManager;
		this.meterRegistry = meterRegistry;
	}

	@Override
	public boolean supportsContext(Observation.Context context) {
		return context instanceof QueryContext;
	}

	@Override
	public void onStart(QueryContext context) {
		Timer.Sample sample = Timer.start(this.meterRegistry);
		context.put(Timer.Sample.class, sample);
	}

	@Override
	public void onStop(QueryContext context) {
		OpenTelemetryAttributes attributes = this.attributesManager.getOrCreateAttributes(context);

		List<Tag> tags = new ArrayList<>();
		addTagIfAvailable(tags, OpenTelemetryQueryObservationConvention.DB_SYSTEM_NAME, attributes::getSystemName);
		addTagIfAvailable(tags, OpenTelemetryQueryObservationConvention.DB_COLLECTION_NAME,
				attributes::getCollectionName);
		addTagIfAvailable(tags, OpenTelemetryQueryObservationConvention.DB_NAMESPACE, attributes::getNamespace);
		addTagIfAvailable(tags, OpenTelemetryQueryObservationConvention.DB_OPERATION_NAME,
				attributes::getOperationName);
		addTagIfAvailable(tags, OpenTelemetryQueryObservationConvention.DB_RESPONSE_STATUS_CODE,
				attributes::getResponseStatusCode);
		addTagIfAvailable(tags, OpenTelemetryQueryObservationConvention.ERROR_TYPE, attributes::getErrorType);
		addTagIfAvailable(tags, OpenTelemetryQueryObservationConvention.DB_QUERY_SUMMARY, attributes::getQuerySummary);
		addTagIfAvailable(tags, OpenTelemetryQueryObservationConvention.DB_QUERY_TEXT, attributes::getQueryText);
		addTagIfAvailable(tags, OpenTelemetryQueryObservationConvention.DB_STORED_PROCEDURE_NAME,
				attributes::getStoredProcedureName);
		addTagIfAvailable(tags, OpenTelemetryQueryObservationConvention.SERVER_ADDRESS, attributes::getServerAddress);
		addTagIfAvailable(tags, OpenTelemetryQueryObservationConvention.SERVER_PORT, attributes::getServerPort);

		addTagIfAvailable(tags, "network.peer.address", context::getHost);
		addTagIfAvailable(tags, "network.peer.port", () -> String.valueOf(context.getPort()));

		Timer.Sample sample = context.getRequired(Timer.Sample.class);
		Timer timer = Timer.builder(DURATION)
			.description("Duration of database client operations")
			.publishPercentileHistogram()
			// @formatter:off
			// ExplicitBucketBoundaries of [ 0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1, 5, 10 ].
			.sla(
				Duration.ofMillis(1),
				Duration.ofMillis(5),
				Duration.ofMillis(10),
				Duration.ofMillis(50),
				Duration.ofMillis(100),
				Duration.ofMillis(500),
				Duration.ofSeconds(1),
				Duration.ofSeconds(5),
				Duration.ofSeconds(10))
			// @formatter:on
			.tags(tags)
			.register(this.meterRegistry);
		sample.stop(timer);
	}

	private void addTagIfAvailable(List<Tag> list, String key, Supplier<String> valueSupplier) {
		String value = this.attributesOverrides.getOrDefault(key, valueSupplier.get());
		if (value != null) {
			list.add(Tag.of(key, value));
		}
	}

	public void setAttributesOverrides(Map<String, String> attributesOverrides) {
		this.attributesOverrides = attributesOverrides;
	}

}
