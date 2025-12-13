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

package net.ttddyy.observation.boot.autoconfigure.opentelemetry;

import net.ttddyy.dsproxy.StatementType;
import net.ttddyy.observation.tracing.opentelemetry.OpenTelemetryQueryAnalyzer;
import net.ttddyy.observation.tracing.opentelemetry.QueryAnalysisResult;
import org.springframework.util.ConcurrentLruCache;

/**
 * Cache {@link QueryAnalysisResult} from {@link OpenTelemetryQueryAnalyzer}.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.3.0
 **/
public class OpenTelemetryQueryAnalyzerCache implements OpenTelemetryQueryAnalyzer {

	private final OpenTelemetryQueryAnalyzer delegate;

	private final ConcurrentLruCache<CacheKey, QueryAnalysisResult> cache;

	public OpenTelemetryQueryAnalyzerCache(OpenTelemetryQueryAnalyzer delegate, int cacheCapacity) {
		this.delegate = delegate;
		this.cache = new ConcurrentLruCache<>(cacheCapacity,
				(key) -> this.delegate.analyze(key.query(), key.isBatch(), key.statementType()));
	}

	@Override
	public QueryAnalysisResult analyze(String query, boolean isBatch, StatementType statementType) {
		return this.cache.get(new CacheKey(query, isBatch, statementType));
	}

	public OpenTelemetryQueryAnalyzer getDelegate() {
		return this.delegate;
	}

	record CacheKey(String query, boolean isBatch, StatementType statementType) {
		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass())
				return false;

			CacheKey cacheKey = (CacheKey) o;
			return query.equals(cacheKey.query);
		}

		@Override
		public int hashCode() {
			return query.hashCode();
		}
	}

}
