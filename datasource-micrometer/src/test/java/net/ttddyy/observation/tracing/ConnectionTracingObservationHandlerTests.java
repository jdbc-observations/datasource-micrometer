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
