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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OpenTelemetrySpanNameBuilder}.
 *
 * @author Tadaya Tsuyukubo
 */
class OpenTelemetrySpanNameBuilderTests {

	@Test
	void withSummary() {
		OpenTelemetryAttributes attributes = createAttributes();
		attributes.setQuerySummary("my summary");
		String result = new OpenTelemetrySpanNameBuilder().build(attributes);
		assertThat(result).isEqualTo("my summary");
	}

	@Test
	void operationNameOnly() {
		OpenTelemetryAttributes attributes = createAttributes();
		attributes.setOperationName("op");
		String result = new OpenTelemetrySpanNameBuilder().build(attributes);
		assertThat(result).isEqualTo("op");
	}

	@Test
	void systemNameOnly() {
		OpenTelemetryAttributes attributes = createAttributes();
		String result = new OpenTelemetrySpanNameBuilder().build(attributes);
		assertThat(result).isEqualTo("system");
	}

	@Test
	void targetOnly() {
		OpenTelemetryAttributes attributes;
		String result;
		OpenTelemetrySpanNameBuilder builder = new OpenTelemetrySpanNameBuilder();

		attributes = createAttributes();
		attributes.setCollectionName("collection");
		result = builder.build(attributes);
		assertThat(result).isEqualTo("collection");

		attributes = createAttributes();
		attributes.setStoredProcedureName("storedProcedure");
		result = builder.build(attributes);
		assertThat(result).isEqualTo("storedProcedure");

		attributes = createAttributes();
		attributes.setNamespace("namespace");
		result = builder.build(attributes);
		assertThat(result).isEqualTo("namespace");

		attributes = createAttributes();
		attributes.setServerAddress("localhost");
		attributes.setServerPort("99");
		result = builder.build(attributes);
		assertThat(result).isEqualTo("localhost:99");
	}

	@Test
	void operationNameAndTarget() {
		OpenTelemetryAttributes attributes;
		String result;
		OpenTelemetrySpanNameBuilder builder = new OpenTelemetrySpanNameBuilder();

		attributes = createAttributes();
		attributes.setOperationName("op");
		attributes.setCollectionName("collection");
		result = builder.build(attributes);
		assertThat(result).isEqualTo("op collection");

		attributes = createAttributes();
		attributes.setOperationName("op");
		attributes.setStoredProcedureName("storedProcedure");
		result = builder.build(attributes);
		assertThat(result).isEqualTo("op storedProcedure");

		attributes = createAttributes();
		attributes.setOperationName("op");
		attributes.setNamespace("namespace");
		result = builder.build(attributes);
		assertThat(result).isEqualTo("op namespace");

		attributes = createAttributes();
		attributes.setOperationName("op");
		attributes.setServerAddress("localhost");
		attributes.setServerPort("99");
		result = builder.build(attributes);
		assertThat(result).isEqualTo("op localhost:99");
	}

	private OpenTelemetryAttributes createAttributes() {
		OpenTelemetryAttributes attributes = new OpenTelemetryAttributes();
		attributes.setSystemName("system"); // system name is always available
		return attributes;
	}

}
