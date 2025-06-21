/*
 * Copyright 2022-2025 the original author or authors.
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

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.sql.DataSource;

import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.test.simple.SimpleTracer;
import io.micrometer.tracing.test.simple.TracerAssert;
import net.ttddyy.dsproxy.ConnectionInfo;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.StatementType;
import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import net.ttddyy.dsproxy.proxy.ParameterSetOperation;
import net.ttddyy.dsproxy.proxy.ProxyConfig;
import net.ttddyy.observation.tracing.ConnectionAttributesManager.ConnectionAttributes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static io.micrometer.tracing.test.simple.TracingAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DataSourceObservationListener}.
 *
 * @author Tadaya Tsuyukubo
 */
class DataSourceObservationListenerTests {

	private SimpleTracer tracer;

	private ObservationRegistry registry;

	@BeforeEach
	void setup() {
		this.tracer = new SimpleTracer();
		this.registry = TestObservationRegistry.create();
	}

	@AfterEach
	void cleanUp() {
		// Some of the tests do not close the scope. To prevent leakage, make sure to
		// close the scope if exists.
		Observation.Scope scope = this.registry.getCurrentObservationScope();
		if (scope != null) {
			scope.close();
		}
	}

	@Test
	void query() throws Exception {
		this.registry.observationConfig().observationHandler(new DefaultTracingObservationHandler(this.tracer));
		DataSourceObservationListener listener = new DataSourceObservationListener(this.registry);

		Method execute = Statement.class.getMethod("execute", String.class);

		QueryInfo queryInfo = new QueryInfo();
		queryInfo.setQuery("SELECT 1");

		ExecutionInfo executionInfo = new ExecutionInfo();
		executionInfo.setConnectionId("id-1");
		executionInfo.setDataSourceName("myDS");
		executionInfo.setMethod(execute);
		List<QueryInfo> queryInfos = Arrays.asList(queryInfo);

		listener.beforeQuery(executionInfo, queryInfos);
		assertThat(tracer.currentSpan()).isNotNull();
		listener.afterQuery(executionInfo, queryInfos);
		assertThat(tracer.currentSpan()).isNull();

		assertThat(tracer).onlySpan()
			.hasNameEqualTo("query")
			.hasTag("jdbc.query[0]", "SELECT 1")
			.doesNotHaveTagWithKey("jdbc.row-affected")
			.doesNotHaveTagWithKey("jdbc.params[0]");
	}

	@Test
	void queryPredicateGetsExpectedData() throws Exception {
		ObservationPredicate testQueryObservationPredicate = (observationName, observationContext) -> {
			if (observationContext instanceof QueryContext) {
				QueryContext queryContext = (QueryContext) observationContext;
				List<String> queries = queryContext.getQueries();

				assertThat(queries).isNotEmpty();
				assertThat(queries.get(0)).isEqualTo("SELECT 1");

				assertThat(queryContext.getDataSourceName()).isEqualTo("myDS");
				return true;
			}
			return true;
		};

		this.registry.observationConfig().observationHandler(new DefaultTracingObservationHandler(this.tracer));
		this.registry.observationConfig().observationPredicate(testQueryObservationPredicate);
		DataSourceObservationListener listener = new DataSourceObservationListener(this.registry);

		Method execute = Statement.class.getMethod("execute", String.class);

		QueryInfo queryInfo = new QueryInfo();
		queryInfo.setQuery("SELECT 1");

		ExecutionInfo executionInfo = new ExecutionInfo();
		executionInfo.setConnectionId("id-1");
		executionInfo.setDataSourceName("myDS");
		executionInfo.setMethod(execute);
		List<QueryInfo> queryInfos = Collections.singletonList(queryInfo);

		listener.beforeQuery(executionInfo, queryInfos);
	}

	@Test
	void queryParametersWithPrepared() throws Exception {
		this.registry.observationConfig().observationHandler(new DefaultTracingObservationHandler(this.tracer));
		DataSourceObservationListener listener = new DataSourceObservationListener(this.registry);
		listener.setIncludeParameterValues(true);

		Method execute = Statement.class.getMethod("execute", String.class);

		Method setInt = PreparedStatement.class.getMethod("setInt", int.class, int.class);
		ParameterSetOperation paramFirst = new ParameterSetOperation(setInt, new Object[] { 1, 100 });
		ParameterSetOperation paramSecond = new ParameterSetOperation(setInt, new Object[] { 1, 200 });
		List<ParameterSetOperation> paramsFirst = Arrays.asList(paramFirst);
		List<ParameterSetOperation> paramsSecond = Arrays.asList(paramSecond);

		QueryInfo queryInfo = new QueryInfo();
		queryInfo.setQuery("SELECT 1 FROM emp WHERE id = ?");
		queryInfo.getParametersList().add(paramsFirst);
		queryInfo.getParametersList().add(paramsSecond);

		ExecutionInfo executionInfo = new ExecutionInfo();
		executionInfo.setConnectionId("id-1");
		executionInfo.setDataSourceName("myDS");
		executionInfo.setMethod(execute);
		executionInfo.setStatementType(StatementType.PREPARED);
		List<QueryInfo> queryInfos = Arrays.asList(queryInfo);

		listener.beforeQuery(executionInfo, queryInfos);
		listener.afterQuery(executionInfo, queryInfos);

		assertThat(tracer).onlySpan().hasNameEqualTo("query").hasTag("jdbc.params[0]", "(100),(200)");
	}

	@Test
	void queryParametersWithCallable() throws Exception {
		this.registry.observationConfig().observationHandler(new DefaultTracingObservationHandler(this.tracer));
		DataSourceObservationListener listener = new DataSourceObservationListener(this.registry);
		listener.setIncludeParameterValues(true);

		Method execute = Statement.class.getMethod("execute", String.class);

		Method setInt = CallableStatement.class.getMethod("setInt", String.class, int.class);
		Method setString = CallableStatement.class.getMethod("setString", String.class, String.class);

		// first parameters
		ParameterSetOperation setIntParamFirst = new ParameterSetOperation(setInt, new Object[] { "id", 100 });
		ParameterSetOperation setStringParamFirst = new ParameterSetOperation(setString,
				new Object[] { "name", "foo" });
		List<ParameterSetOperation> paramsFirst = Arrays.asList(setIntParamFirst, setStringParamFirst);

		// second parameters
		ParameterSetOperation setIntParamSecond = new ParameterSetOperation(setInt, new Object[] { "id", 200 });
		ParameterSetOperation setStringParamSecond = new ParameterSetOperation(setString,
				new Object[] { "name", "bar" });
		List<ParameterSetOperation> paramsSecond = Arrays.asList(setIntParamSecond, setStringParamSecond);

		QueryInfo queryInfo = new QueryInfo();
		queryInfo.setQuery("{call my_func(:id, :name)");
		queryInfo.getParametersList().add(paramsFirst);
		queryInfo.getParametersList().add(paramsSecond);

		ExecutionInfo executionInfo = new ExecutionInfo();
		executionInfo.setConnectionId("id-1");
		executionInfo.setDataSourceName("myDS");
		executionInfo.setMethod(execute);
		executionInfo.setStatementType(StatementType.CALLABLE);
		List<QueryInfo> queryInfos = Arrays.asList(queryInfo);

		listener.beforeQuery(executionInfo, queryInfos);
		listener.afterQuery(executionInfo, queryInfos);

		assertThat(tracer).onlySpan()
			.hasNameEqualTo("query")
			.hasTag("jdbc.params[0]", "(id=100,name=foo),(id=200,name=bar)");
	}

	@Test
	void queryConnectionContext() throws Exception {
		ObservationPredicate testConnectionObservationPredicate = (observationName, observationContext) -> {
			if (observationContext instanceof ConnectionContext) {
				ConnectionContext connectionContext = (ConnectionContext) observationContext;
				assertThat(connectionContext.getHost()).isEqualTo("localhost");
				assertThat(connectionContext.getPort()).isEqualTo(5555);
				assertThat(connectionContext.getDataSourceName()).isEqualTo("myDS");
				return true;
			}
			return true;
		};

		this.registry.observationConfig().observationHandler(new QueryTracingObservationHandler(this.tracer));
		this.registry.observationConfig().observationPredicate(testConnectionObservationPredicate);

		DataSourceObservationListener listener = new DataSourceObservationListener(this.registry);

		ConnectionInfo connectionInfo = new ConnectionInfo();
		connectionInfo.setDataSourceName("myDS");

		ConnectionAttributes connectionAttributes = new ConnectionAttributes();
		connectionAttributes.connectionInfo = connectionInfo;
		connectionAttributes.host = "localhost";
		connectionAttributes.port = 5555;
		ConnectionAttributesManager connectionAttributesManager = mock(ConnectionAttributesManager.class);
		given(connectionAttributesManager.get("id-1")).willReturn(connectionAttributes);
		listener.setConnectionAttributesManager(connectionAttributesManager);

		Method execute = Statement.class.getMethod("execute", String.class);

		ExecutionInfo executionInfo = new ExecutionInfo();
		executionInfo.setConnectionId("id-1");
		executionInfo.setDataSourceName("myDS");
		executionInfo.setMethod(execute);
		List<QueryInfo> queryInfos = new ArrayList<>();

		listener.beforeQuery(executionInfo, queryInfos);
		listener.afterQuery(executionInfo, queryInfos);

		assertThat(tracer).onlySpan()
			.hasRemoteServiceNameEqualTo("myDS")
			.hasIpEqualTo("localhost")
			.hasPortEqualTo(5555);
	}

	@ParameterizedTest
	@MethodSource
	void queryRowAffected(Method method, Object result, String expected) {
		this.registry.observationConfig().observationHandler(new DefaultTracingObservationHandler(this.tracer));
		DataSourceObservationListener listener = new DataSourceObservationListener(this.registry);

		ExecutionInfo executionInfo = new ExecutionInfo();
		executionInfo.setConnectionId("id-1");
		executionInfo.setDataSourceName("myDS");
		executionInfo.setMethod(method);
		executionInfo.setResult(result);
		List<QueryInfo> queryInfos = new ArrayList<>();

		listener.beforeQuery(executionInfo, queryInfos);
		listener.afterQuery(executionInfo, queryInfos);

		assertThat(tracer).onlySpan().hasTag("jdbc.row-affected", expected);
	}

	static Stream<Arguments> queryRowAffected() throws Exception {
		Method executeUpdate = Statement.class.getMethod("executeUpdate", String.class);
		Method executeLargeUpdate = Statement.class.getMethod("executeLargeUpdate", String.class);
		Method executeBatch = Statement.class.getMethod("executeBatch");
		Method executeLargeBatch = Statement.class.getMethod("executeLargeBatch");
		// @formatter:off
		return Stream.of(
				Arguments.of(executeUpdate, 99, "99"),
				Arguments.of(executeLargeUpdate, (long) 999, "999"),
				Arguments.of(executeBatch, new int[] { 1, 2, 3 }, "[1, 2, 3]"),
				Arguments.of(executeLargeBatch, new long[] { 10, 20, 30 }, "[10, 20, 30]"));
		// @formatter:on
	}

	@Test
	void connection() throws Exception {
		this.registry.observationConfig().observationHandler(new ConnectionTracingObservationHandler(this.tracer));

		DataSourceObservationListener listener = new DataSourceObservationListener(this.registry);

		Method getConnection = DataSource.class.getMethod("getConnection");
		Method closeMethod = Connection.class.getMethod("close");

		DatabaseMetaData metaData = mock(DatabaseMetaData.class);
		Connection connection = mock(Connection.class);
		given(connection.getMetaData()).willReturn(metaData);
		given(metaData.getURL()).willReturn("jdbc:mysql://localhost:5555/mydatabase");

		ProxyConfig proxyConfig = new ProxyConfig.Builder().dataSourceName("myDS").build();

		ConnectionInfo connectionInfo = new ConnectionInfo();
		connectionInfo.setConnectionId("id-1");
		connectionInfo.setDataSourceName("myDS");

		MethodExecutionContext executionContext = new MethodExecutionContext();
		executionContext.setConnectionInfo(connectionInfo);
		executionContext.setMethod(getConnection);
		executionContext.setTarget(mock(DataSource.class));
		executionContext.setResult(connection);
		executionContext.setProxyConfig(proxyConfig);

		listener.beforeMethod(executionContext);
		assertThat(tracer.currentSpan()).isNotNull();
		listener.afterMethod(executionContext);
		assertThat(tracer.currentSpan()).isNotNull();

		// "getConnection" starts a span but it will not end until "Connection#close".
		assertThat(tracer.currentSpan()).isNotEnded();
		assertThat(tracer.getSpans()).hasSize(1);

		// "Connection#close()" will close the span
		MethodExecutionContext secondExecutionContext = new MethodExecutionContext();
		secondExecutionContext.setConnectionInfo(connectionInfo);
		secondExecutionContext.setMethod(closeMethod);
		secondExecutionContext.setTarget(mock(Connection.class));

		listener.beforeMethod(secondExecutionContext);
		assertThat(tracer.currentSpan()).isNotNull();
		listener.afterMethod(secondExecutionContext);
		assertThat(tracer.currentSpan()).isNull();

		assertThat(this.tracer).onlySpan()
			.hasNameEqualTo("connection")
			.hasRemoteServiceNameEqualTo("myDS")
			.hasIpEqualTo("localhost")
			.hasPortEqualTo(5555)
			.hasEventWithNameEqualTo("acquired");
	}

	@Test
	void getConnectionFailure() throws Exception {
		this.registry.observationConfig().observationHandler(new ConnectionTracingObservationHandler(this.tracer));
		// gh-44: make sure datasource is available in observation filter
		this.registry.observationConfig().observationFilter((context) -> {
			DataSource ds = ((DataSourceBaseContext) context).getDataSource();
			assertThat(ds).as("datasource needs to be available in observation filter").isNotNull();
			return context;
		});

		Method getConnection = DataSource.class.getMethod("getConnection");
		RuntimeException exception = new RuntimeException();

		ProxyConfig proxyConfig = new ProxyConfig.Builder().dataSourceName("myDS").build();

		// when getConnection failed, result is null and thrown has exception
		MethodExecutionContext executionContext = new MethodExecutionContext();
		executionContext.setMethod(getConnection);
		executionContext.setTarget(mock(DataSource.class));
		executionContext.setThrown(exception);
		executionContext.setProxyConfig(proxyConfig);

		DataSourceObservationListener listener = new DataSourceObservationListener(this.registry);
		listener.beforeMethod(executionContext);
		assertThat(tracer.currentSpan()).isNotNull();
		listener.afterMethod(executionContext);
		// when getConnection failed, it closes the connection span.
		assertThat(tracer.currentSpan()).isNull();
		assertThat(tracer.getSpans()).hasSize(1);

		// @formatter:off
		assertThat(this.tracer).onlySpan()
				.hasNameEqualTo("connection")
				.doesNotHaveRemoteServiceNameEqualTo("myDS")
				.hasIpThatIsBlank()
				.doesNotHavePortEqualTo(5555)
				.doesNotHaveEventWithNameEqualTo("acquired")
				.assertThatThrowable()
				.isSameAs(exception);
		// @formatter:on
	}

	@Test
	void commitAndRollback() throws Exception {
		this.registry.observationConfig().observationHandler(new ConnectionTracingObservationHandler(this.tracer));

		Method commitMethod = Connection.class.getMethod("commit");
		Method rollbackMethod = Connection.class.getMethod("rollback");

		Observation observation = Observation.start("test", ConnectionContext::new, this.registry);

		ConnectionAttributes connectionAttributes = new ConnectionAttributes();
		connectionAttributes.scope = observation.openScope();

		ConnectionAttributesManager connectionAttributesManager = new DefaultConnectionAttributesManager();
		connectionAttributesManager.put("id-1", connectionAttributes);

		DataSourceObservationListener listener = new DataSourceObservationListener(this.registry);
		listener.setConnectionAttributesManager(connectionAttributesManager);

		ConnectionInfo connectionInfo = new ConnectionInfo();
		connectionInfo.setConnectionId("id-1");

		MethodExecutionContext commitExecutionContext = new MethodExecutionContext();
		commitExecutionContext.setConnectionInfo(connectionInfo);
		commitExecutionContext.setMethod(commitMethod);
		commitExecutionContext.setTarget(mock(Connection.class));

		listener.afterMethod(commitExecutionContext);

		assertThat(this.tracer.currentSpan()).hasEventWithNameEqualTo("commit");

		// check rollback
		MethodExecutionContext rollbackExecutionContext = new MethodExecutionContext();
		rollbackExecutionContext.setConnectionInfo(connectionInfo);
		rollbackExecutionContext.setMethod(rollbackMethod);
		rollbackExecutionContext.setTarget(mock(Connection.class));

		listener.afterMethod(rollbackExecutionContext);

		assertThat(this.tracer.currentSpan()).hasEventWithNameEqualTo("rollback");
	}

	@Test
	void resultSetRowCount() throws Exception {
		// Verify result set observation populates jdbc.row-count tag

		this.registry.observationConfig().observationHandler(new ResultSetTracingObservationHandler(this.tracer));
		DataSourceObservationListener listener = new DataSourceObservationListener(this.registry);

		ResultSet resultSet = mock(ResultSet.class);
		Statement statement = mock(Statement.class);
		given(resultSet.getStatement()).willReturn(statement);

		ConnectionInfo connectionInfo = new ConnectionInfo();
		connectionInfo.setConnectionId("id-1");
		connectionInfo.setDataSourceName("myDS");

		ConnectionAttributes connectionAttributes = new ConnectionAttributes();
		ConnectionAttributesManager connectionAttributesManager = new DefaultConnectionAttributesManager();
		connectionAttributes.connectionInfo = connectionInfo;
		connectionAttributesManager.put("id-1", connectionAttributes);
		listener.setConnectionAttributesManager(connectionAttributesManager);

		MethodExecutionContext nextExecutionContext = new MethodExecutionContext();
		nextExecutionContext.setConnectionInfo(connectionInfo);
		nextExecutionContext.setMethod(ResultSet.class.getMethod("next"));
		nextExecutionContext.setTarget(resultSet);
		nextExecutionContext.setResult(Boolean.TRUE);

		// simulate the result has two records
		listener.afterMethod(nextExecutionContext);
		listener.afterMethod(nextExecutionContext);

		MethodExecutionContext closeExecutionContext = new MethodExecutionContext();
		closeExecutionContext.setConnectionInfo(connectionInfo);
		closeExecutionContext.setMethod(ResultSet.class.getMethod("close"));
		closeExecutionContext.setTarget(resultSet);

		// ResultSet#close should stop the observation which writes the tag to the span.
		listener.afterMethod(closeExecutionContext);

		TracerAssert.assertThat(this.tracer).onlySpan().hasTag("jdbc.row-count", "2");
	}

	@Test
	void resultSetObservation() throws Exception {
		AtomicReference<Observation.Context> contextHolder = new AtomicReference<>();

		this.registry.observationConfig().observationHandler(new ResultSetTracingObservationHandler(this.tracer));
		this.registry.observationConfig().observationHandler(context -> {
			contextHolder.set(context);
			return true;
		});

		DataSourceObservationListener listener = new DataSourceObservationListener(this.registry);

		ResultSet resultSet = mock(ResultSet.class);
		Statement statement = mock(Statement.class);
		given(resultSet.getStatement()).willReturn(statement);

		ConnectionInfo connectionInfo = new ConnectionInfo();
		connectionInfo.setConnectionId("id-1");
		connectionInfo.setDataSourceName("myDS");

		createResultSetObservation(listener, connectionInfo, resultSet, true);

		// verify context has datasource-name
		assertThat(contextHolder).hasValueSatisfying((context) -> {
			assertThat(context).isInstanceOfSatisfying(ResultSetContext.class, (rsContext) -> {
				assertThat(rsContext.getDataSourceName()).isEqualTo("myDS");
				assertThat(rsContext.getOperations()).hasSize(1)
					.first()
					.extracting(ResultSetOperation::getMethod)
					.extracting(Method::getName)
					.isEqualTo("next");
			});
		});

		// ResultSet#close should stop the observation
		MethodExecutionContext closeExecutionContext = createResultSetCloseMethodExecutionContext(connectionInfo,
				resultSet);
		listener.afterMethod(closeExecutionContext);

		assertThat(tracer.currentSpan()).isNull();
		TracerAssert.assertThat(this.tracer).onlySpan().hasTag("jdbc.row-count", "1");
	}

	@Test
	void resultSetObservationWithEmptyResultSet() throws Exception {
		this.registry.observationConfig().observationHandler(new ResultSetTracingObservationHandler(this.tracer));
		DataSourceObservationListener listener = new DataSourceObservationListener(this.registry);

		ResultSet resultSet = mock(ResultSet.class);
		Statement statement = mock(Statement.class);
		given(resultSet.getStatement()).willReturn(statement);

		ConnectionInfo connectionInfo = new ConnectionInfo();
		connectionInfo.setConnectionId("id-1");
		connectionInfo.setDataSourceName("myDS");

		createResultSetObservation(listener, connectionInfo, resultSet, false);

		MethodExecutionContext closeExecutionContext = createResultSetCloseMethodExecutionContext(connectionInfo,
				resultSet);

		// ResultSet#close should stop the observation
		listener.afterMethod(closeExecutionContext);
		assertThat(tracer.currentSpan()).isNull();

		TracerAssert.assertThat(this.tracer).onlySpan().hasTag("jdbc.row-count", "0");
	}

	private void createResultSetObservation(DataSourceObservationListener listener, ConnectionInfo connectionInfo,
			ResultSet resultSet, boolean hasNext) throws Exception {
		Method nextMethod = ResultSet.class.getMethod("next");

		ConnectionAttributes connectionAttributes = new ConnectionAttributes();
		ConnectionAttributesManager connectionAttributesManager = new DefaultConnectionAttributesManager();
		connectionAttributes.connectionInfo = connectionInfo;
		connectionAttributesManager.put("id-1", connectionAttributes);

		listener.setConnectionAttributesManager(connectionAttributesManager);

		MethodExecutionContext nextExecutionContext = new MethodExecutionContext();
		nextExecutionContext.setConnectionInfo(connectionInfo);
		nextExecutionContext.setMethod(nextMethod);
		nextExecutionContext.setTarget(resultSet);
		nextExecutionContext.setResult(hasNext);

		listener.afterMethod(nextExecutionContext);
		assertThat(tracer.currentSpan()).isNotNull();
	}

	private MethodExecutionContext createResultSetCloseMethodExecutionContext(ConnectionInfo connectionInfo,
			ResultSet resultSet) throws Exception {
		Method closeMethod = ResultSet.class.getMethod("close");
		MethodExecutionContext closeExecutionContext = new MethodExecutionContext();
		closeExecutionContext.setConnectionInfo(connectionInfo);
		closeExecutionContext.setMethod(closeMethod);
		closeExecutionContext.setTarget(resultSet);
		return closeExecutionContext;
	}

	@Test
	void resultSetObservationClosedByStatement() throws Exception {
		this.registry.observationConfig().observationHandler(new ResultSetTracingObservationHandler(this.tracer));
		DataSourceObservationListener listener = new DataSourceObservationListener(this.registry);

		ConnectionInfo connectionInfo = new ConnectionInfo();
		connectionInfo.setConnectionId("id-1");
		connectionInfo.setDataSourceName("myDS");

		ResultSet resultSet = mock(ResultSet.class);
		Statement statement = mock(Statement.class);
		given(resultSet.getStatement()).willReturn(statement);

		createResultSetObservation(listener, connectionInfo, resultSet, true);

		// ResultSet#close may be skipped. In such case, Statement#close should
		// implicitly close the ResultSet.
		MethodExecutionContext closeExecutionContext = createResultSetCloseMethodExecutionContext(connectionInfo,
				resultSet);

		// Statement#close should stop the ResultSet observation
		listener.afterMethod(closeExecutionContext);
		assertThat(tracer.currentSpan()).isNull();
	}

	@Test
	void resultSetObservationClosedByConnection() throws Exception {
		this.registry.observationConfig().observationHandler(new ResultSetTracingObservationHandler(this.tracer));
		DataSourceObservationListener listener = new DataSourceObservationListener(this.registry);

		ConnectionInfo connectionInfo = new ConnectionInfo();
		connectionInfo.setConnectionId("id-1");
		connectionInfo.setDataSourceName("myDS");

		ResultSet resultSet = mock(ResultSet.class);
		Statement statement = mock(Statement.class);
		given(resultSet.getStatement()).willReturn(statement);

		createResultSetObservation(listener, connectionInfo, resultSet, true);

		// [ResultSet|Statement]#close may be skipped. In such case, Connection#close
		// should implicitly close the ResultSet.
		MethodExecutionContext closeExecutionContext = createResultSetCloseMethodExecutionContext(connectionInfo,
				resultSet);

		// Connection#close should stop the ResultSet observation
		listener.afterMethod(closeExecutionContext);
		assertThat(tracer.currentSpan()).isNull();
	}

	/**
	 * Test to reproduce NPE thrown from
	 * {@link DataSourceObservationListener#handleResultSetNext(MethodExecutionContext)}
	 * when {@link MethodExecutionContext#getResult()} returns {@code  null}
	 * @see <a href=
	 * "https://github.com/jdbc-observations/datasource-micrometer/issues/22">Issue 22</a>
	 */
	@Test
	void resultSetObservationWithNullResult() throws Exception {
		this.registry.observationConfig().observationHandler(new ResultSetTracingObservationHandler(this.tracer));
		DataSourceObservationListener listener = new DataSourceObservationListener(this.registry);

		ResultSet resultSet = mock(ResultSet.class);

		ConnectionInfo connectionInfo = new ConnectionInfo();
		connectionInfo.setConnectionId("id-1");
		connectionInfo.setDataSourceName("myDS");

		Method closeMethod = ResultSet.class.getMethod("next");
		MethodExecutionContext nextExecutionContext = new MethodExecutionContext();
		nextExecutionContext.setConnectionInfo(connectionInfo);
		nextExecutionContext.setMethod(closeMethod);
		nextExecutionContext.setTarget(resultSet);
		// Result is explicitly null
		// Listener should not produce NPE
		nextExecutionContext.setResult(null);

		createResultSetObservation(listener, connectionInfo, resultSet, false);

		assertThatNoException().isThrownBy(() -> listener.afterMethod(nextExecutionContext));

		// ResultSet#next should not stop the observation
		assertThat(tracer.currentSpan()).isNotNull();

		TracerAssert.assertThat(this.tracer).reportedSpans().hasSize(1);
	}

	@Test
	void resultSetObservationWithMultipleClose() throws Exception {
		this.registry.observationConfig().observationHandler(new ResultSetTracingObservationHandler(this.tracer));
		DataSourceObservationListener listener = new DataSourceObservationListener(this.registry);

		ResultSet resultSet = mock(ResultSet.class);

		ConnectionInfo connectionInfo = new ConnectionInfo();
		connectionInfo.setConnectionId("id-1");
		connectionInfo.setDataSourceName("myDS");

		Statement statement = mock(Statement.class);
		given(resultSet.getStatement()).willReturn(statement);

		createResultSetObservation(listener, connectionInfo, resultSet, true);

		// call "close"
		listener.afterMethod(createResultSetCloseMethodExecutionContext(connectionInfo, resultSet));

		// call "close" again
		listener.afterMethod(createResultSetCloseMethodExecutionContext(connectionInfo, resultSet));

		TracerAssert.assertThat(this.tracer).reportedSpans().hasSize(1);
	}

	@Test
	void resultSetObservationWithIncludeResultSetOperationsFalse() throws Exception {
		AtomicReference<Observation.Context> contextHolder = new AtomicReference<>();
		this.registry.observationConfig().observationHandler(new ResultSetTracingObservationHandler(this.tracer));
		this.registry.observationConfig().observationHandler(context -> {
			contextHolder.set(context);
			return true;
		});

		DataSourceObservationListener listener = new DataSourceObservationListener(this.registry);
		listener.setIncludeResultSetOperations(false);

		ResultSet resultSet = mock(ResultSet.class);

		ConnectionInfo connectionInfo = new ConnectionInfo();
		connectionInfo.setConnectionId("id-1");
		connectionInfo.setDataSourceName("myDS");

		Statement statement = mock(Statement.class);
		given(resultSet.getStatement()).willReturn(statement);

		createResultSetObservation(listener, connectionInfo, resultSet, true);

		// verify context has datasource-name
		assertThat(contextHolder).hasValueSatisfying((context) -> {
			assertThat(context).isInstanceOfSatisfying(ResultSetContext.class, (rsContext) -> {
				assertThat(rsContext.getOperations()).hasSize(0);
			});
		});

		// call "close"
		listener.afterMethod(createResultSetCloseMethodExecutionContext(connectionInfo, resultSet));
	}

}
