package net.ttddyy.observation.tracing;

import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;

/**
 * @author Tadaya Tsuyukubo
 */

public class QueryTracingObservationHandler extends DefaultTracingObservationHandler {

	public QueryTracingObservationHandler(Tracer tracer) {
		super(tracer);
	}

	@Override
	public void onStart(Observation.Context context) {
		// TODO: add "remoteIpAndPort" and "remoteServiceName".

	}

	@Override
	public boolean supportsContext(Context context) {
		return context instanceof QueryContext;
	}

}
