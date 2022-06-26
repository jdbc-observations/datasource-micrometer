package net.ttddyy.observation.tracing;

import io.micrometer.observation.Observation.Context;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;

/**
 * @author Tadaya Tsuyukubo
 */

public class ResultSetTracingObservationHandler extends DefaultTracingObservationHandler {

	public ResultSetTracingObservationHandler(Tracer tracer) {
		super(tracer);
	}

	@Override
	public boolean supportsContext(Context context) {
		return context instanceof ResultSetContext;
	}

	@Override
	public void tagSpan(Context context, Span span) {
		super.tagSpan(context, span);

		ResultSetContext resultSetContext = (ResultSetContext) context;
		if (resultSetContext.getHost() != null) {
			span.remoteIpAndPort(resultSetContext.getHost(), resultSetContext.getPort());
		}
		span.remoteServiceName(resultSetContext.getDataSourceName());
	}

}
