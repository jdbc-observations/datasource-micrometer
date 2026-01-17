/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ttddyy.observation.tracing.opentelemetry;

import io.micrometer.common.lang.Nullable;

import java.net.URI;

/**
 * Default implementation of {@link DatabaseNamespaceRetriever}.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.3.0
 */
public class SimpleDatabaseNamespaceRetriever implements DatabaseNamespaceRetriever {

	@Nullable
	@Override
	public String retrieve(String url) {
		URI uri;
		try {
			// strip "jdbc:", Remove all white space according to RFC 2396;
			uri = URI.create(url.substring(5).replace(" ", ""));
		}
		catch (Exception ex) {
			return null;
		}

		String path = uri.getPath();
		return path.startsWith("/") ? path.substring(1) : path;
	}

}
