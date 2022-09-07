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

import java.util.Collections;
import java.util.List;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Span.Builder;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.propagation.Propagator;

/**
 * A base class of tracing observation handler for JDBC operations.
 *
 * @author Tadaya Tsuyukubo
 */
public abstract class DataSourceBaseObservationHandler
		extends PropagatingSenderTracingObservationHandler<DataSourceBaseContext> {

	private static final NoopPropagator NOOP_PROPAGATOR = new NoopPropagator();

	public DataSourceBaseObservationHandler(Tracer tracer) {
		// The handler is implemented as "PropagatingSenderTracingObservationHandler" to
		// specify span kind to client; however, db operations do not need to propagate
		// anything about observation. Therefore, here creates an empty propagator.
		super(tracer, NOOP_PROPAGATOR);
	}

	@Override
	public void customizeSenderSpan(DataSourceBaseContext context, Span span) {
		if (context.getHost() != null) {
			span.remoteIpAndPort(context.getHost(), context.getPort());
		}
	}

	static class NoopPropagator implements Propagator {

		@Override
		public List<String> fields() {
			return Collections.emptyList();
		}

		@Override
		public <C> void inject(TraceContext context, C carrier, Setter<C> setter) {
			// no-op
		}

		@Override
		public <C> Builder extract(C carrier, Getter<C> getter) {
			throw new UnsupportedOperationException();
		}

	}

}
