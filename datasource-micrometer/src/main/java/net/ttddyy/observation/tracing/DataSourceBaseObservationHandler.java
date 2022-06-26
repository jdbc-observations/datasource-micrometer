package net.ttddyy.observation.tracing;

import io.micrometer.observation.Observation.Context;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;

/**
 * @author Tadaya Tsuyukubo
 */
public abstract class DataSourceBaseObservationHandler extends DefaultTracingObservationHandler {

	public DataSourceBaseObservationHandler(Tracer tracer) {
		super(tracer);
	}

	@Override
	public void tagSpan(Context context, Span span) {
		super.tagSpan(context, span);

		DataSourceBaseContext baseContext = (DataSourceBaseContext) context;
		if (baseContext.getHost() != null) {
			span.remoteIpAndPort(baseContext.getHost(), baseContext.getPort());
		}
		span.remoteServiceName(baseContext.getDataSourceName());
	}

}
