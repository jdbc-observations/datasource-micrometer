package net.ttddyy.observation.tracing;

import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import io.micrometer.common.lang.Nullable;
import io.micrometer.common.util.internal.logging.InternalLogger;
import io.micrometer.common.util.internal.logging.InternalLoggerFactory;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import net.ttddyy.dsproxy.ConnectionInfo;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import net.ttddyy.dsproxy.listener.MethodExecutionListener;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.observation.tracing.ConnectionAttributesManager.ConnectionAttributes;
import net.ttddyy.observation.tracing.ConnectionAttributesManager.ResultSetAttributes;
import net.ttddyy.observation.tracing.JdbcObservation.QueryHighCardinalityKeyNames;


/**
 * @author Tadaya Tsuyukubo
 */
public class DataSourceObservationListener implements QueryExecutionListener, MethodExecutionListener {

	private static final InternalLogger logger = InternalLoggerFactory.getInstance(DataSourceObservationListener.class);

	private final ObservationRegistry observationRegistry;

	private ConnectionAttributesManager connectionAttributesManager = new DefaultConnectionAttributesManager();

	private ConnectionKeyValuesProvider connectionKeyValuesProvider = new ConnectionKeyValuesProvider() {};

	private QueryKeyValuesProvider queryKeyValuesProvider = new QueryKeyValuesProvider() {};

	private ResultSetKeyValuesProvider resultSetKeyValuesProvider = new ResultSetKeyValuesProvider() {};

	public DataSourceObservationListener(ObservationRegistry observationRegistry) {
		this.observationRegistry = observationRegistry;
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
		populateSharedInfo(queryContext, executionInfo.getConnectionId());

		Observation observation = JdbcObservation.QUERY.observation(this.observationRegistry, queryContext)
				.keyValuesProvider(this.queryKeyValuesProvider)
				.start();

		if (logger.isDebugEnabled()) {
			logger.debug("Created a new child observation before query [" + observation + "]");
		}
		tagQueries(queryInfoList, observation);
		executionInfo.addCustomValue(Observation.Scope.class.getName(), observation.openScope());
	}

	private void tagQueries(List<QueryInfo> queryInfoList, Observation observation) {
		int i = 0;
		for (QueryInfo queryInfo : queryInfoList) {
			observation.highCardinalityKeyValue(String.format(QueryHighCardinalityKeyNames.QUERY.getKeyName(), i), queryInfo.getQuery());
			i++;
		}
	}

	private void populateSharedInfo(DataSourceBaseContext context, String connectionId) {
		ConnectionAttributes connectionAttributes = this.connectionAttributesManager.get(connectionId);
		if (connectionAttributes != null) {
			context.setHost(connectionAttributes.host);
			context.setPort(connectionAttributes.port);
			context.setDataSourceName(connectionAttributes.connectionInfo.getDataSourceName());
		}
	}

	private void stopQueryObservation(ExecutionInfo executionInfo) {
		boolean hasRowCount = executionInfo.getMethod().getName().equals("executeUpdate") && executionInfo.getThrowable() == null;
		Observation.Scope scopeToUse = executionInfo.getCustomValue(Observation.Scope.class.getName(), Observation.Scope.class);
		if (scopeToUse == null) {
			return;
		}
		try (Observation.Scope scope = scopeToUse) {
			Observation observation = scope.getCurrentObservation();
			if (logger.isDebugEnabled()) {
				logger.debug("Continued the child observation in after query [" + observation + "]");
			}
			if (hasRowCount) {
				int rowCount = (int) executionInfo.getResult();
				observation.highCardinalityKeyValue(QueryHighCardinalityKeyNames.ROW_COUNT.getKeyName(), String.valueOf(rowCount));
			}
			Throwable throwable = executionInfo.getThrowable();
			if (throwable != null) {
				observation.error(throwable);
			}
			observation.stop();
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

		Observation observation = JdbcObservation.CONNECTION.observation(this.observationRegistry, connectionContext)
				.keyValuesProvider(this.connectionKeyValuesProvider)
				.start();
		executionContext.addCustomValue(Observation.Scope.class.getName(), observation.openScope());
	}

	private void handleGetConnectionAfter(MethodExecutionContext executionContext) {
		String dataSourceName = executionContext.getConnectionInfo().getDataSourceName();
		Connection connection = (Connection) executionContext.getResult();
		URI connectionUrl = getConnectionUrl(connection);

		ConnectionContext connectionContext = executionContext.getCustomValue(ConnectionContext.class.getName(), ConnectionContext.class);
		connectionContext.setDataSourceName(dataSourceName);
		if (connectionUrl != null) {
			connectionContext.setHost(connectionUrl.getHost());
			connectionContext.setPort(connectionUrl.getPort());
		}

		Observation.Scope scopeToUse = executionContext.getCustomValue(Observation.Scope.class.getName(), Observation.Scope.class);

		Throwable throwable = executionContext.getThrown();
		if (throwable != null && scopeToUse != null) {
			try (Observation.Scope scope = scopeToUse) {
				Observation observation = scope.getCurrentObservation();
				observation.error(throwable);
				observation.stop();
				// for normal case, observation is stopped when connection is closed.
				// see "handleConnectionClose()".
				return;
			}
		}

		ConnectionInfo connectionInfo = executionContext.getConnectionInfo();

		ConnectionAttributes connectionAttributes = new ConnectionAttributes();
		connectionAttributes.connectionInfo = connectionInfo;
		connectionAttributes.scope = scopeToUse;
		connectionAttributes.connectionContext = connectionContext;
		if (connectionUrl != null) {
			connectionAttributes.host = connectionUrl.getHost();
			connectionAttributes.port = connectionUrl.getPort();
		}

		String connectionId = connectionInfo.getConnectionId();
		this.connectionAttributesManager.put(connectionId, connectionAttributes);

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
			Observation observation = scope.getCurrentObservation();
			Throwable throwable = executionContext.getThrown();
			if (throwable != null) {
				observation.error(throwable);
			}
			observation.stop();
		}

	}

	private void handleConnectionCommit(MethodExecutionContext executionContext) {
		String connectionId = executionContext.getConnectionInfo().getConnectionId();
		ConnectionAttributes connectionAttributes = this.connectionAttributesManager.get(connectionId);
		if (connectionAttributes == null) {
			return;
		}
		connectionAttributes.connectionContext.setCommitAt(Instant.now());
	}

	private void handleConnectionRollback(MethodExecutionContext executionContext) {
		String connectionId = executionContext.getConnectionInfo().getConnectionId();
		ConnectionAttributes connectionAttributes = this.connectionAttributesManager.get(connectionId);
		if (connectionAttributes == null) {
			return;
		}
		connectionAttributes.connectionContext.setRollbackAt(Instant.now());
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
			ResultSetAttributes resultSetAttributes = connectionAttributes.resultSetAttributesManager.getByResultSet(resultSet);
			if (resultSetAttributes == null) {
				// new ResultSet observation
				ResultSetContext resultSetContext = new ResultSetContext();
				populateSharedInfo(resultSetContext, executionContext.getConnectionInfo().getConnectionId());
				Observation observation = JdbcObservation.RESULT_SET.observation(this.observationRegistry, resultSetContext)
						.keyValuesProvider(this.resultSetKeyValuesProvider)
						.start();

				if (logger.isDebugEnabled()) {
					logger.debug("Created a new result-set observation [" + observation + "]");
				}

				resultSetAttributes = new ResultSetAttributes();
				resultSetAttributes.scope = observation.openScope();
				resultSetAttributes.context = resultSetContext;

				Statement statement = null;
				try {
					// retrieve statement and associate it with ResultSet. It is used to close
					// associated ResultSets when Statement is closed without closing
					// ResultSets. See "handleStatementClosed()".
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
	 * jdbc:mysql://localhost:5555/mydatabase}.
	 * Taken from Spring Cloud Sleuth.
	 */
	@Nullable
	private URI getConnectionUrl(Connection connection) {
		URI url = null;
		try {
			String urlAsString = connection.getMetaData().getURL().substring(5); // strip "jdbc:"
			url = URI.create(urlAsString.replace(" ", "")); // Remove all white space
			// according to RFC 2396;
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

		// The proper step is close ResultSet, then close Statement. However, JDBC API allows
		// to close Statement without ResultSet. In such case, ResultSet should be closed.
		// If it happens, here makes sure all associated ResultSet observations get stopped.
		Statement statement = (Statement) executionContext.getTarget();
		Set<ResultSetAttributes> resultSetAttributes = connectionAttributes.resultSetAttributesManager.removeByStatement(statement);
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
		ResultSetAttributes resultSetAttributes = connectionAttributes.resultSetAttributesManager.removeByResultSet(resultSet);
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
			Observation observation = scope.getCurrentObservation();
			if (throwable != null) {
				observation.error(throwable);
			}
			observation.stop();
		}
	}

	public void setConnectionAttributesManager(ConnectionAttributesManager connectionAttributesManager) {
		this.connectionAttributesManager = connectionAttributesManager;
	}

	public void setConnectionKeyValuesProvider(ConnectionKeyValuesProvider connectionKeyValuesProvider) {
		this.connectionKeyValuesProvider = connectionKeyValuesProvider;
	}

	public void setQueryKeyValuesProvider(QueryKeyValuesProvider queryKeyValuesProvider) {
		this.queryKeyValuesProvider = queryKeyValuesProvider;
	}

	public void setResultSetKeyValuesProvider(ResultSetKeyValuesProvider resultSetKeyValuesProvider) {
		this.resultSetKeyValuesProvider = resultSetKeyValuesProvider;
	}
}
