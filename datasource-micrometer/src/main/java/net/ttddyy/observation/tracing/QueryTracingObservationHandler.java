package net.ttddyy.observation.tracing;

import io.micrometer.observation.Observation.Context;
import io.micrometer.tracing.Tracer;

/**
 * @author Tadaya Tsuyukubo
 */

public class QueryTracingObservationHandler extends DataSourceBaseObservationHandler {

	public QueryTracingObservationHandler(Tracer tracer) {
		super(tracer);
	}

	@Override
	public boolean supportsContext(Context context) {
		return context instanceof QueryContext;
	}

}
