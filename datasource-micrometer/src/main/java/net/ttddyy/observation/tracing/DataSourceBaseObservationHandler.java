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
