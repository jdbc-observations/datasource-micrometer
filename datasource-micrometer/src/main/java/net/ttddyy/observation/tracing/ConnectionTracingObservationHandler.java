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

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import io.micrometer.observation.Observation.Context;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

/**
 * @author Tadaya Tsuyukubo
 */

public class ConnectionTracingObservationHandler extends DataSourceBaseObservationHandler {

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

		// commit
		Instant commitAt = connectionContext.getCommitAt();
		if (commitAt != null) {
			span.event("commit", TimeUnit.SECONDS.toNanos(commitAt.getEpochSecond()) + commitAt.getNano(),
					TimeUnit.NANOSECONDS);
		}

		// rollback
		Instant rollbackAt = connectionContext.getRollbackAt();
		if (rollbackAt != null) {
			span.event("rollback", TimeUnit.SECONDS.toNanos(rollbackAt.getEpochSecond()) + rollbackAt.getNano(),
					TimeUnit.NANOSECONDS);
		}

	}

}
