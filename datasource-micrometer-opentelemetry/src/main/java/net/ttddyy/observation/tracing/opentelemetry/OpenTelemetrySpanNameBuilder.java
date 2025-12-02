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

/**
 * A strategy to build a span name following the OpenTelemetry Semantic Conventions.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.3.0
 **/
public class OpenTelemetrySpanNameBuilder {

	public String build(OpenTelemetryAttributes attributes) {
		if (attributes.getQuerySummary() != null) {
			return attributes.getQuerySummary();
		}
		StringBuilder sb = new StringBuilder();
		if (attributes.getOperationName() != null) {
			sb.append(attributes.getOperationName());
		}
		sb.append(composeTarget(attributes));

		if (sb.length() > 0) {
			sb.append(attributes.getSystemName());
		}
		return sb.toString();
	}

	private String composeTarget(OpenTelemetryAttributes attributes) {
		if (attributes.getCollectionName() != null) {
			return attributes.getCollectionName();
		}
		if (attributes.getStoredProcedureName() != null) {
			return attributes.getStoredProcedureName();
		}
		if (attributes.getNamespace() != null) {
			return attributes.getNamespace();
		}
		if (attributes.getServerAddress() != null && attributes.getServerPort() != null) {
			return attributes.getServerAddress() + ":" + attributes.getServerPort();
		}
		return "";
	}

}
