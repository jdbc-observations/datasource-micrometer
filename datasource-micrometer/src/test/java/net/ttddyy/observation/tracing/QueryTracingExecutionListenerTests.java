package net.ttddyy.observation.tracing;

import java.util.Arrays;
import java.util.List;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.test.simple.SimpleTracer;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import org.junit.jupiter.api.Test;

import static io.micrometer.tracing.test.simple.TracerAssert.assertThat;

/**
 * @author Tadaya Tsuyukubo
 */
class QueryTracingExecutionListenerTests {

	@Test
	void spans() {
		SimpleTracer tracer = new SimpleTracer();
		ObservationRegistry registry = ObservationRegistry.create();
		registry.observationConfig().observationHandler(new DefaultTracingObservationHandler(tracer));

		QueryTracingExecutionListener listener = new QueryTracingExecutionListener(registry);

		QueryInfo queryInfo = new QueryInfo();
		queryInfo.setQuery("SELECT 1");

		ExecutionInfo executionInfo = new ExecutionInfo();
		List<QueryInfo> queryInfos = Arrays.asList(queryInfo);

		listener.beforeQuery(executionInfo, queryInfos);
		listener.afterQuery(executionInfo, queryInfos);

		assertThat(tracer)
				.onlySpan()
				.hasNameEqualTo("query")
				.hasTag("jdbc.query[0]", "SELECT 1");
	}

}
