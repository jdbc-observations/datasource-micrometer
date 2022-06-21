package net.ttddyy.observation.tracing;

import java.util.List;

import io.micrometer.common.util.internal.logging.InternalLogger;
import io.micrometer.common.util.internal.logging.InternalLoggerFactory;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.observation.tracing.JdbcObservation.QueryHighCardinalityKeyNames;


/**
 * @author Tadaya Tsuyukubo
 */
public class QueryTracingExecutionListener implements QueryExecutionListener {

	InternalLogger logger = InternalLoggerFactory.getInstance(QueryTracingExecutionListener.class);

	private final ObservationRegistry observationRegistry;

	private QueryKeyValuesProvider queryKeyValuesProvider = new QueryKeyValuesProvider() {};

	public QueryTracingExecutionListener(ObservationRegistry observationRegistry) {
		this.observationRegistry = observationRegistry;
	}

	// TODO: retrieve connection info at "getConnection()" and use it for query spans (url, etc)

	@Override
	public void beforeQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
		Observation observation = childObservation();
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

	private Observation childObservation() {
		QueryContext queryContext = new QueryContext();

		return Observation.createNotStarted(JdbcObservation.QUERY.getName(), queryContext, this.observationRegistry)
				.contextualName(JdbcObservation.QUERY.getContextualName())
				.keyValuesProvider(this.queryKeyValuesProvider)
				.start();
	}

	@Override
	public void afterQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
		Observation.Scope scopeToUse = executionInfo.getCustomValue(Observation.Scope.class.getName(), Observation.Scope.class);
		if (scopeToUse == null) {
			return;
		}

		try (Observation.Scope scope = scopeToUse) {
			Observation observation = scope.getCurrentObservation();
			if (logger.isDebugEnabled()) {
				logger.debug("Continued the child observation in after query [" + observation + "]");
			}
			final Throwable throwable = executionInfo.getThrowable();
			if (throwable != null) {
				observation.error(throwable);
			}
			observation.stop();
		}

//		if (execInfo.getMethod().getName().equals("executeUpdate") && execInfo.getThrowable() == null) {
//			this.strategy.addQueryRowCount(execInfo.getConnectionId(), execInfo.getStatement(),
//					(int) execInfo.getResult());
//		}
//		String sql = queryInfoList.stream().map(QueryInfo::getQuery).collect(Collectors.joining("\n"));
//		this.strategy.afterQuery(execInfo.getConnectionId(), execInfo.getStatement(), sql, execInfo.getThrowable());
	}

	public void setQueryKeyValuesProvider(QueryKeyValuesProvider queryKeyValuesProvider) {
		this.queryKeyValuesProvider = queryKeyValuesProvider;
	}
}
