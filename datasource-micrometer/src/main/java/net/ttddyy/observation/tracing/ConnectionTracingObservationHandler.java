package net.ttddyy.observation.tracing;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import io.micrometer.observation.Observation.Context;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;

/**
 * @author Tadaya Tsuyukubo
 */

public class ConnectionTracingObservationHandler extends DefaultTracingObservationHandler {

	public ConnectionTracingObservationHandler(Tracer tracer) {
		super(tracer);
	}

	@Override
	public boolean supportsContext(Context context) {
		return context instanceof ConnectionContext;
	}

	@Override
	public void tagSpan(Context context, Span span) {
		super.tagSpan(context, span);

		ConnectionContext connectionContext = (ConnectionContext) context;
		if (connectionContext.getHost() != null) {
			span.remoteIpAndPort(connectionContext.getHost(), connectionContext.getPort());
		}
		span.remoteServiceName(connectionContext.getDataSourceName());

		// commit
		Instant commitAt = connectionContext.getCommitAt();
		if (commitAt != null) {
			span.event("commit", TimeUnit.SECONDS.toNanos(commitAt.getEpochSecond()) + commitAt.getNano(), TimeUnit.NANOSECONDS);
		}

		// rollback
		Instant rollbackAt = connectionContext.getRollbackAt();
		if (rollbackAt != null) {
			span.event("rollback", TimeUnit.SECONDS.toNanos(rollbackAt.getEpochSecond()) + rollbackAt.getNano(), TimeUnit.NANOSECONDS);
		}

	}
}
