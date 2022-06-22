package net.ttddyy.observation.tracing;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.test.simple.SimpleTracer;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import net.ttddyy.dsproxy.proxy.ProxyConfig;
import org.junit.jupiter.api.Test;

import static io.micrometer.tracing.test.simple.SpanAssert.assertThat;
import static io.micrometer.tracing.test.simple.TracerAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Tadaya Tsuyukubo
 */
class QueryTracingExecutionListenerTests {

	@Test
	void query() {
		SimpleTracer tracer = new SimpleTracer();
		ObservationRegistry registry = ObservationRegistry.create();
		registry.observationConfig().observationHandler(new DefaultTracingObservationHandler(tracer));

		QueryTracingExecutionListener listener = new QueryTracingExecutionListener(registry);

		QueryInfo queryInfo = new QueryInfo();
		queryInfo.setQuery("SELECT 1");

		ExecutionInfo executionInfo = new ExecutionInfo();
		List<QueryInfo> queryInfos = Arrays.asList(queryInfo);

		listener.beforeQuery(executionInfo, queryInfos);
		assertThat(tracer.currentSpan()).isNotNull();
		listener.afterQuery(executionInfo, queryInfos);
		assertThat(tracer.currentSpan()).isNull();

		assertThat(tracer)
				.onlySpan()
				.hasNameEqualTo("query")
				.hasTag("jdbc.query[0]", "SELECT 1");
	}

	@Test
	void connection() throws Exception {
		SimpleTracer tracer = new SimpleTracer();
		ObservationRegistry registry = ObservationRegistry.create();
		registry.observationConfig().observationHandler(new ConnectionTracingObservationHandler(tracer));

		QueryTracingExecutionListener listener = new QueryTracingExecutionListener(registry);

		ProxyConfig proxyConfig = ProxyConfig.Builder.create().dataSourceName("myDS").build();
		Method getConnection = DataSource.class.getMethod("getConnection");

		DatabaseMetaData metaData = mock(DatabaseMetaData.class);
		Connection connection = mock(Connection.class);
		given(connection.getMetaData()).willReturn(metaData);
		given(metaData.getURL()).willReturn("jdbc:mysql://localhost:5555/mydatabase");

		MethodExecutionContext executionContext = new MethodExecutionContext();
		executionContext.setProxyConfig(proxyConfig);
		executionContext.setMethod(getConnection);
		executionContext.setTarget(mock(DataSource.class));
		executionContext.setResult(connection);

		listener.beforeMethod(executionContext);
		assertThat(tracer.currentSpan()).isNotNull();
		listener.afterMethod(executionContext);
		assertThat(tracer.currentSpan()).isNull();

		assertThat(tracer)
				.onlySpan()
				.hasNameEqualTo("connection")
				.hasRemoteServiceNameEqualTo("myDS")
				.hasIpEqualTo("localhost")
				.hasPortEqualTo(5555)
		;
	}

}
