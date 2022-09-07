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

import io.micrometer.observation.Observation.Context;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingObservationHandler;

/**
 * A {@link TracingObservationHandler} for connection.
 *
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
	public void customizeSenderSpan(DataSourceBaseContext context, Span span) {
		super.customizeSenderSpan(context, span);
		// A observation is created before executing "getConnection" method.
		// PropagatingSenderTracingObservationHandler populates the remote
		// service name at "onStart"("createSenderSpan"). However, the remote service name
		// is determined after "getConnection" is called.
		// To reflect the populated remote-service-name, here (called at "onStop") sets
		// the value.
		span.remoteServiceName(context.getRemoteServiceName());
	}

}
