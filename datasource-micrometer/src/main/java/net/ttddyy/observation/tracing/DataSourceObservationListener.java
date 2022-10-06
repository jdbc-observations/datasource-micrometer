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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.sql.DataSource;

import io.micrometer.common.lang.Nullable;
import io.micrometer.common.util.internal.logging.InternalLogger;
import io.micrometer.common.util.internal.logging.InternalLoggerFactory;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.ObservationRegistry;
import net.ttddyy.dsproxy.ConnectionInfo;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import net.ttddyy.dsproxy.listener.MethodExecutionListener;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.observation.tracing.ConnectionAttributesManager.ConnectionAttributes;
import net.ttddyy.observation.tracing.ConnectionAttributesManager.ResultSetAttributes;
import net.ttddyy.observation.tracing.JdbcObservation.JdbcEvents;

/**
 * Datasource-proxy listener implementation for JDBC observation.
 *
 * @author Tadaya Tsuyukubo
 */
public class DataSourceObservationListener implements QueryExecutionListener, MethodExecutionListener {

	private static final InternalLogger logger = InternalLoggerFactory.getInstance(DataSourceObservationListener.class);

	private final Supplier<ObservationRegistry> observationRegistrySupplier;

	private ConnectionAttributesManager connectionAttributesManager = new DefaultConnectionAttributesManager();

	private ConnectionObservationConvention connectionObservationConvention = new ConnectionObservationConvention() {
	};

	private QueryObservationConvention queryObservationConvention = new QueryObservationConvention() {
	};

	private ResultSetObservationConvention resultSetObservationConvention = new ResultSetObservationConvention() {
	};

	private QueryParametersSpanTagProvider queryParametersSpanTagProvider = new DefaultQueryParametersSpanTagProvider();

	/**
	 * Whether to tag query parameter values.
	 */
	private boolean includeParameterValues;

	public DataSourceObservationListener(ObservationRegistry observationRegistry) {
		this(() -> observationRegistry);
	}

	// This constructor takes a supplier to lazily resolve the observation registry.
	// This is purely a workaround to break a circular reference for spring boot usage.
	// The circular reference happens when the `MeterRegistryPostProcessor` creates
	// `MeterBinder` when `MeterRegistry` is created. If this circular reference is
	// solved in Spring Boot, there is no need to use supplier for the observation
	// registry.
	public DataSourceObservationListener(Supplier<ObservationRegistry> observationRegistry) {
		this.observationRegistrySupplier = observationRegistry;
	}

	@Override
	public void beforeQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
		startQueryObservation(executionInfo, queryInfoList);
	}

	@Override
	public void afterQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
		stopQueryObservation(executionInfo);
	}

	private void startQueryObservation(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
		QueryContext queryContext = new QueryContext();
		executionInfo.addCustomValue(QueryContext.class.getName(), queryContext);
		populateFromConnectionAttributes(queryContext, executionInfo.getConnectionId());

		Observation observation = createAndStartObservation(JdbcObservation.QUERY, queryContext,
				this.queryObservationConvention);

		if (logger.isDebugEnabled()) {
			logger.debug("Created a new child observation before query [" + observation + "]");
		}
		populateQueryContext(executionInfo, queryInfoList, queryContext);
		executionInfo.addCustomValue(Observation.Scope.class.getName(), observation.openScope());
	}

	private Observation createAndStartObservation(JdbcObservation observationType, DataSourceBaseContext context,
			ObservationConvention<? extends Context> observationConvention) {
		return observationType.observation(this.observationRegistrySupplier.get(), () -> context)
				.observationConvention(observationConvention).start();
	}

	private void populateQueryContext(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList,
			QueryContext context) {
		for (QueryInfo queryInfo : queryInfoList) {
			context.getQueries().add(queryInfo.getQuery());
			if (this.includeParameterValues) {
				String params = this.queryParametersSpanTagProvider.getParameters(executionInfo, queryInfoList);
				context.getParams().add(params);
			}
		}
	}

	private void populateFromConnectionAttributes(DataSourceBaseContext context, String connectionId) {
		ConnectionAttributes connectionAttributes = this.connectionAttributesManager.get(connectionId);
		if (connectionAttributes != null) {
			context.setHost(connectionAttributes.host);
			context.setPort(connectionAttributes.port);
			context.setRemoteServiceName(connectionAttributes.connectionInfo.getDataSourceName());
			context.setDataSource(connectionAttributes.dataSource);
		}
	}

	private void stopQueryObservation(ExecutionInfo executionInfo) {
		String methodName = executionInfo.getMethod().getName();
		boolean hasAffectedRowCount = executionInfo.getThrowable() == null
				&& ("executeUpdate".equals(methodName) || "executeLargeUpdate".equals(methodName)
						|| "executeBatch".equals(methodName) || "executeLargeBatch".equals(methodName));
		Observation.Scope scopeToUse = executionInfo.getCustomValue(Observation.Scope.class.getName(),
				Observation.Scope.class);
		if (scopeToUse == null) {
			return;
		}
		try (Observation.Scope scope = scopeToUse) {
			Observation observation = scope.getCurrentObservation();
			if (logger.isDebugEnabled()) {
				logger.debug("Continued the child observation in after query [" + observation + "]");
			}
			if (hasAffectedRowCount) {
				String affectedRowCount;
				Object result = executionInfo.getResult();
				if ("executeUpdate".equals(methodName) || "executeLargeUpdate".equals(methodName)) {
					affectedRowCount = String.valueOf(result);
				}
				else if ("executeBatch".equals(methodName)) {
					affectedRowCount = Arrays.toString((int[]) result);
				}
				else {
					affectedRowCount = Arrays.toString((long[]) result);
				}
				QueryContext queryContext = executionInfo.getCustomValue(QueryContext.class.getName(),
						QueryContext.class);
				queryContext.setAffectedRowCount(affectedRowCount);
			}
			stopObservation(observation, executionInfo.getThrowable());
		}
	}

	@Override
	public void beforeMethod(MethodExecutionContext executionContext) {
		String methodName = executionContext.getMethod().getName();
		Object target = executionContext.getTarget();
		if (target instanceof DataSource && "getConnection".equals(methodName)) {
			handleGetConnectionBefore(executionContext);
		}
	}

	@Override
	public void afterMethod(MethodExecutionContext executionContext) {
		String methodName = executionContext.getMethod().getName();
		Object target = executionContext.getTarget();
		if (target instanceof DataSource && "getConnection".equals(methodName)) {
			handleGetConnectionAfter(executionContext);
		}
		else if (target instanceof Connection) {
			if ("close".equals(methodName)) {
				handleConnectionClose(executionContext);
			}
			else if ("commit".equals(methodName)) {
				handleConnectionCommit(executionContext);
			}
			else if ("rollback".equals(methodName)) {
				handleConnectionRollback(executionContext);
			}
		}
		else if (target instanceof Statement) {
			if ("close".equals(methodName)) {
				handleStatementClose(executionContext);
			}
		}
		else if (target instanceof ResultSet) {
			if ("close".equals(methodName)) {
				handleResultSetClose(executionContext);
			}
			else if ("next".equals(methodName)) {
				handleResultSetNext(executionContext);
			}
		}
	}

	private void handleGetConnectionBefore(MethodExecutionContext executionContext) {
		ConnectionContext connectionContext = new ConnectionContext();
		executionContext.addCustomValue(ConnectionContext.class.getName(), connectionContext);

		Observation observation = createAndStartObservation(JdbcObservation.CONNECTION, connectionContext,
				this.connectionObservationConvention);
		executionContext.addCustomValue(Observation.Scope.class.getName(), observation.openScope());
	}

	private void handleGetConnectionAfter(MethodExecutionContext executionContext) {
		DataSource dataSource = (DataSource) executionContext.getTarget();
		Connection connection = (Connection) executionContext.getResult();
		URI connectionUrl = getConnectionUrl(connection);

		Observation.Scope scopeToUse = executionContext.getCustomValue(Observation.Scope.class.getName(),
				Observation.Scope.class);
		ConnectionInfo connectionInfo = executionContext.getConnectionInfo();

		ConnectionAttributes connectionAttributes = new ConnectionAttributes();
		connectionAttributes.connectionInfo = connectionInfo;
		connectionAttributes.scope = scopeToUse;
		connectionAttributes.dataSource = dataSource;
		if (connectionUrl != null) {
			connectionAttributes.host = connectionUrl.getHost();
			connectionAttributes.port = connectionUrl.getPort();
		}

		String connectionId = connectionInfo.getConnectionId();
		this.connectionAttributesManager.put(connectionId, connectionAttributes);

		ConnectionContext connectionContext = executionContext.getCustomValue(ConnectionContext.class.getName(),
				ConnectionContext.class);
		populateFromConnectionAttributes(connectionContext, connectionId);

		Throwable throwable = executionContext.getThrown();
		if (throwable != null && scopeToUse != null) {
			// Handle closing the observation due to an error from getConnection().
			// For normal case, observation is stopped when connection is closed.
			// see "handleConnectionClose()".
			try (Observation.Scope scope = scopeToUse) {
				this.connectionAttributesManager.remove(connectionId);
				stopObservation(scope.getCurrentObservation(), throwable);
			}
		}

		// When "getConnection" was successful, a connection is acquired.
		if (scopeToUse != null) {
			scopeToUse.getCurrentObservation().event(JdbcEvents.CONNECTION_ACQUIRED);
		}
	}

	private void handleConnectionClose(MethodExecutionContext executionContext) {
		String connectionId = executionContext.getConnectionInfo().getConnectionId();
		ConnectionAttributes connectionAttributes = this.connectionAttributesManager.remove(connectionId);
		if (connectionAttributes == null) {
			return;
		}

		// In case, Statement/ResultSet were not closed, close associated observation here
		Set<ResultSetAttributes> resultSetAttributes = connectionAttributes.resultSetAttributesManager.removeAll();
		for (ResultSetAttributes resultSetAttribute : resultSetAttributes) {
			stopResultSetObservation(resultSetAttribute.scope, executionContext.getThrown());
		}

		// Stop connection observation
		Observation.Scope scopeToUse = connectionAttributes.scope;
		if (scopeToUse == null) {
			return;
		}
		try (Observation.Scope scope = scopeToUse) {
			stopObservation(scope.getCurrentObservation(), executionContext.getThrown());
		}
	}

	private void stopObservation(Observation observation, @Nullable Throwable throwable) {
		if (throwable != null) {
			observation.error(throwable);
		}
		observation.stop();
	}

	private void handleConnectionCommit(MethodExecutionContext executionContext) {
		String connectionId = executionContext.getConnectionInfo().getConnectionId();
		ConnectionAttributes connectionAttributes = this.connectionAttributesManager.get(connectionId);
		if (connectionAttributes == null) {
			return;
		}
		Observation.Scope scopeToUse = connectionAttributes.scope;
		if (scopeToUse == null) {
			return;
		}
		scopeToUse.getCurrentObservation().event(JdbcEvents.CONNECTION_COMMIT);
	}

	private void handleConnectionRollback(MethodExecutionContext executionContext) {
		String connectionId = executionContext.getConnectionInfo().getConnectionId();
		ConnectionAttributes connectionAttributes = this.connectionAttributesManager.get(connectionId);
		if (connectionAttributes == null) {
			return;
		}
		Observation.Scope scopeToUse = connectionAttributes.scope;
		if (scopeToUse == null) {
			return;
		}
		scopeToUse.getCurrentObservation().event(JdbcEvents.CONNECTION_ROLLBACK);
	}

	private void handleResultSetNext(MethodExecutionContext executionContext) {
		String connectionId = executionContext.getConnectionInfo().getConnectionId();
		ConnectionAttributes connectionAttributes = this.connectionAttributesManager.get(connectionId);
		if (connectionAttributes == null) {
			return;
		}

		Boolean hasNext = (Boolean) executionContext.getResult();
		ResultSet resultSet = (ResultSet) executionContext.getTarget();
		if (hasNext) {
			ResultSetAttributes resultSetAttributes = connectionAttributes.resultSetAttributesManager
					.getByResultSet(resultSet);
			if (resultSetAttributes == null) {
				// new ResultSet observation
				ResultSetContext resultSetContext = new ResultSetContext();
				populateFromConnectionAttributes(resultSetContext,
						executionContext.getConnectionInfo().getConnectionId());
				Observation observation = createAndStartObservation(JdbcObservation.RESULT_SET, resultSetContext,
						this.resultSetObservationConvention);

				if (logger.isDebugEnabled()) {
					logger.debug("Created a new result-set observation [" + observation + "]");
				}

				resultSetAttributes = new ResultSetAttributes();
				resultSetAttributes.scope = observation.openScope();
				resultSetAttributes.context = resultSetContext;

				Statement statement = null;
				try {
					// retrieve statement and associate it with ResultSet. It is used to
					// close associated ResultSets when Statement is closed without
					// closing ResultSets. See "handleStatementClosed()".
					statement = resultSet.getStatement();
				}
				catch (SQLException exception) {
					// ignore
				}

				connectionAttributes.resultSetAttributesManager.add(resultSet, statement, resultSetAttributes);
			}
			resultSetAttributes.context.incrementCount();
		}
	}

	/**
	 * This attempts to get the ip and port from the JDBC URL. Ex. localhost and 5555 from
	 * {@code
	 * jdbc:mysql://localhost:5555/mydatabase}. Taken from Spring Cloud Sleuth.
	 */
	@Nullable
	private URI getConnectionUrl(Connection connection) {
		URI url = null;
		try {
			// strip "jdbc:"
			String urlAsString = connection.getMetaData().getURL().substring(5);
			// Remove all white space according to RFC 2396;
			url = URI.create(urlAsString.replace(" ", ""));
		}
		catch (Exception ex) {
			// remote address is optional
		}
		return url;
	}

	private void handleStatementClose(MethodExecutionContext executionContext) {
		String connectionId = executionContext.getConnectionInfo().getConnectionId();
		ConnectionAttributes connectionAttributes = this.connectionAttributesManager.get(connectionId);
		if (connectionAttributes == null) {
			return;
		}

		// The proper step is close ResultSet, then close Statement. However, JDBC API
		// allows to close Statement without ResultSet. In such case, ResultSet should be
		// closed. If it happens, here makes sure all associated ResultSet observations
		// get stopped.
		Statement statement = (Statement) executionContext.getTarget();
		Set<ResultSetAttributes> resultSetAttributes = connectionAttributes.resultSetAttributesManager
				.removeByStatement(statement);
		for (ResultSetAttributes resultSetAttribute : resultSetAttributes) {
			stopResultSetObservation(resultSetAttribute.scope, executionContext.getThrown());
		}
	}

	private void handleResultSetClose(MethodExecutionContext executionContext) {
		String connectionId = executionContext.getConnectionInfo().getConnectionId();
		ConnectionAttributes connectionAttributes = this.connectionAttributesManager.get(connectionId);
		if (connectionAttributes == null) {
			return;
		}

		ResultSet resultSet = (ResultSet) executionContext.getTarget();
		ResultSetAttributes resultSetAttributes = connectionAttributes.resultSetAttributesManager
				.removeByResultSet(resultSet);
		if (resultSetAttributes == null) {
			return;
		}

		stopResultSetObservation(resultSetAttributes.scope, executionContext.getThrown());
	}

	private void stopResultSetObservation(@Nullable Observation.Scope scopeToUse, @Nullable Throwable throwable) {
		if (scopeToUse == null) {
			return;
		}
		try (Observation.Scope scope = scopeToUse) {
			stopObservation(scope.getCurrentObservation(), throwable);
		}
	}

	public void setConnectionAttributesManager(ConnectionAttributesManager connectionAttributesManager) {
		this.connectionAttributesManager = connectionAttributesManager;
	}

	public void setConnectionObservationConvention(ConnectionObservationConvention connectionObservationConvention) {
		this.connectionObservationConvention = connectionObservationConvention;
	}

	public void setQueryObservationConvention(QueryObservationConvention queryObservationConvention) {
		this.queryObservationConvention = queryObservationConvention;
	}

	public void setResultSetObservationConvention(ResultSetObservationConvention resultSetObservationConvention) {
		this.resultSetObservationConvention = resultSetObservationConvention;
	}

	public void setQueryParametersSpanTagProvider(QueryParametersSpanTagProvider queryParametersSpanTagProvider) {
		this.queryParametersSpanTagProvider = queryParametersSpanTagProvider;
	}

	public void setIncludeParameterValues(boolean includeParameterValues) {
		this.includeParameterValues = includeParameterValues;
	}

}
