package net.ttddyy.observation.tracing;

import java.net.URI;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;

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
import net.ttddyy.observation.tracing.JdbcObservation.QueryHighCardinalityKeyNames;


/**
 * @author Tadaya Tsuyukubo
 */
public class QueryTracingExecutionListener implements QueryExecutionListener, MethodExecutionListener {

	private static final InternalLogger logger = InternalLoggerFactory.getInstance(QueryTracingExecutionListener.class);

	private final ObservationRegistry observationRegistry;

	private ConnectionAttributesManager connectionAttributesManager = new DefaultConnectionAttributesManager();

	private ConnectionKeyValuesProvider connectionKeyValuesProvider = new ConnectionKeyValuesProvider() {};

	private QueryKeyValuesProvider queryKeyValuesProvider = new QueryKeyValuesProvider() {};

	public QueryTracingExecutionListener(ObservationRegistry observationRegistry) {
		this.observationRegistry = observationRegistry;
	}

	// TODO: retrieve connection info at "getConnection()" and use it for query spans (url, etc)

	@Override
	public void beforeQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
		QueryContext queryContext = new QueryContext();
		queryContext.setDataSourceName(executionInfo.getDataSourceName());

		ConnectionAttributes connectionAttributes = this.connectionAttributesManager.get(executionInfo.getConnectionId());
		if (connectionAttributes != null) {
			queryContext.setUrl(connectionAttributes.connectionUrl);
		}

		Observation observation = Observation.createNotStarted(JdbcObservation.QUERY.getName(), queryContext, this.observationRegistry)
				.contextualName(JdbcObservation.QUERY.getContextualName())
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

	@Override
	public void afterQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
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

			final Throwable throwable = executionInfo.getThrowable();
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
	}

	private void handleGetConnectionBefore(MethodExecutionContext executionContext) {
		ConnectionContext connectionContext = new ConnectionContext();
		executionContext.addCustomValue(ConnectionContext.class.getName(), connectionContext);

		Observation observation = Observation.createNotStarted(JdbcObservation.CONNECTION.getName(), connectionContext, this.observationRegistry)
				.contextualName(JdbcObservation.CONNECTION.getContextualName())
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
		connectionContext.setUrl(connectionUrl);

		Observation.Scope scopeToUse = executionContext.getCustomValue(Observation.Scope.class.getName(), Observation.Scope.class);

		// TODO: thrown error check
		ConnectionInfo connectionInfo = executionContext.getConnectionInfo();

		ConnectionAttributes connectionAttributes = new ConnectionAttributes();
		connectionAttributes.connectionInfo = connectionInfo;
		connectionAttributes.connectionUrl = connectionUrl;
		connectionAttributes.scope = scopeToUse;
		connectionAttributes.connectionContext = connectionContext;

		String connectionId = connectionInfo.getConnectionId();
		this.connectionAttributesManager.put(connectionId, connectionAttributes);

		// TODO: clean up
		final Throwable throwable = executionContext.getThrown();
		if (throwable != null && scopeToUse != null) {
			try (Observation.Scope scope = scopeToUse) {
				Observation observation = scope.getCurrentObservation();
				observation.error(throwable);
				observation.stop();
				// normal case, observation is stopped when connection is closed.
			}
		}
	}

	private void handleConnectionClose(MethodExecutionContext executionContext) {
		String connectionId = executionContext.getConnectionInfo().getConnectionId();
		ConnectionAttributes connectionAttributes = this.connectionAttributesManager.remove(connectionId);
		if (connectionAttributes == null) {
			return;
		}

		Observation.Scope scopeToUse = connectionAttributes.scope;
		if (scopeToUse == null) {
			return;
		}
		try (Observation.Scope scope = scopeToUse) {
			Observation observation = scope.getCurrentObservation();
			final Throwable throwable = executionContext.getThrown();
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

	/**
	 * This attempts to get the ip and port from the JDBC URL. Ex. localhost and 5555 from
	 * {@code
	 * jdbc:mysql://localhost:5555/mydatabase}.
	 * Taken from Spring Cloud Sleuth.
	 */
	private @Nullable URI getConnectionUrl(Connection connection) {
		URI url = null;
		try {
			String urlAsString = connection.getMetaData().getURL().substring(5); // strip "jdbc:"
			url = URI.create(urlAsString.replace(" ", "")); // Remove all white space
			// according to RFC 2396;
		}
		catch (Exception e) {
			// remote address is optional
		}
		return url;
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

}
