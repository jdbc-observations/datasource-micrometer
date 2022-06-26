package net.ttddyy.observation.tracing;

import java.net.URI;

import io.micrometer.observation.Observation.Context;
import io.micrometer.tracing.Span;
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
	public boolean supportsContext(Context context) {
		return context instanceof QueryContext;
	}

	@Override
	public void tagSpan(Context context, Span span) {
		super.tagSpan(context, span);

		QueryContext connectionContext = (QueryContext) context;
		URI url = connectionContext.getUrl();
		if (url != null) {
			span.remoteIpAndPort(url.getHost(), url.getPort());
		}
		span.remoteServiceName(connectionContext.getDataSourceName());
	}

}
