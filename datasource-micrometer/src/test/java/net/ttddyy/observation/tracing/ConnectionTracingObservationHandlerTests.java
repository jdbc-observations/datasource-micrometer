/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ttddyy.observation.tracing;

import java.net.URI;
import java.time.Instant;
import java.util.Map.Entry;

import io.micrometer.tracing.test.simple.SimpleSpan;
import io.micrometer.tracing.test.simple.SimpleTracer;
import org.junit.jupiter.api.Test;

import static io.micrometer.tracing.test.simple.SpanAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConnectionTracingObservationHandler}.
 *
 * @author Tadaya Tsuyukubo
 */
class ConnectionTracingObservationHandlerTests {

	@Test
	void tagSpan() {
		SimpleTracer tracer = new SimpleTracer();
		ConnectionTracingObservationHandler handler = new ConnectionTracingObservationHandler(tracer);

		Instant commitAt = Instant.parse("2022-01-01T01:02:03.004Z");
		Instant rollbackAt = Instant.parse("2022-02-02T00:00:00.00Z");

		ConnectionContext context = new ConnectionContext();
		context.setDataSourceName("myDS");
		context.setHost("localhost");
		context.setPort(5555);
		context.setCommitAt(commitAt);
		context.setRollbackAt(rollbackAt);

		SimpleSpan span = new SimpleSpan();
		handler.tagSpan(context, span);

		assertThat(span)
				.hasRemoteServiceNameEqualTo("myDS")
				.hasIpEqualTo("localhost")
				.hasPortEqualTo(5555)
				.hasEventWithNameEqualTo("commit")
				.hasEventWithNameEqualTo("rollback");
		assertThat(span.getEvents()).hasSize(2)
				.extracting(Entry::getValue)
				.containsExactly("commit", "rollback");
	}
}
