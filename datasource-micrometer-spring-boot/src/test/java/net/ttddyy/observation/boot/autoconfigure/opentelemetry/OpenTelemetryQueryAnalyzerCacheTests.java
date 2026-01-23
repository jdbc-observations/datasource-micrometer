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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link OpenTelemetryQueryAnalyzerCache}.
 */
class OpenTelemetryQueryAnalyzerCacheTests {

	@Test
	void cache() {
		OpenTelemetryQueryAnalyzer delegate = mock(OpenTelemetryQueryAnalyzer.class);
		QueryAnalysisResult fooResult = mock(QueryAnalysisResult.class);
		QueryAnalysisResult barResult = mock(QueryAnalysisResult.class);
		given(delegate.analyze("foo", true, StatementType.STATEMENT)).willReturn(fooResult);
		given(delegate.analyze("bar", true, StatementType.STATEMENT)).willReturn(barResult);
		OpenTelemetryQueryAnalyzerCache cache = new OpenTelemetryQueryAnalyzerCache(delegate, 1);

		QueryAnalysisResult result;

		result = cache.analyze("foo", true, StatementType.STATEMENT);
		verify(delegate, times(1)).analyze("foo", true, StatementType.STATEMENT);
		assertThat(result).isSameAs(fooResult);

		result = cache.analyze("foo", true, StatementType.STATEMENT);
		verify(delegate, times(1)).analyze("foo", true, StatementType.STATEMENT);
		assertThat(result).isSameAs(fooResult);

		result = cache.analyze("bar", true, StatementType.STATEMENT);
		verify(delegate, times(1)).analyze("bar", true, StatementType.STATEMENT);
		assertThat(result).isSameAs(barResult);

		// cache size is 1, so the first cache was already swapped out. calling foo will
		// be cache miss.
		result = cache.analyze("foo", true, StatementType.STATEMENT);
		verify(delegate, times(2)).analyze("foo", true, StatementType.STATEMENT);
		assertThat(result).isSameAs(fooResult);
	}

	@Test
	void cacheKeyEquality() {
		OpenTelemetryQueryAnalyzer delegate = mock(OpenTelemetryQueryAnalyzer.class);
		QueryAnalysisResult foo1 = mock(QueryAnalysisResult.class);
		QueryAnalysisResult foo2 = mock(QueryAnalysisResult.class);
		QueryAnalysisResult foo3 = mock(QueryAnalysisResult.class);
		given(delegate.analyze("foo", true, StatementType.STATEMENT)).willReturn(foo1);
		given(delegate.analyze("foo", false, StatementType.STATEMENT)).willReturn(foo2);
		given(delegate.analyze("foo", true, StatementType.PREPARED)).willReturn(foo3);
		OpenTelemetryQueryAnalyzerCache cache = new OpenTelemetryQueryAnalyzerCache(delegate, 5);

		QueryAnalysisResult result;

		result = cache.analyze("foo", true, StatementType.STATEMENT);
		verify(delegate, times(1)).analyze("foo", true, StatementType.STATEMENT);
		verify(delegate, times(0)).analyze("foo", false, StatementType.STATEMENT);
		verify(delegate, times(0)).analyze("foo", true, StatementType.PREPARED);
		assertThat(result).isSameAs(foo1);

		result = cache.analyze("foo", false, StatementType.STATEMENT);
		verify(delegate, times(1)).analyze("foo", true, StatementType.STATEMENT);
		verify(delegate, times(1)).analyze("foo", false, StatementType.STATEMENT);
		verify(delegate, times(0)).analyze("foo", true, StatementType.PREPARED);
		assertThat(result).isSameAs(foo2);

		result = cache.analyze("foo", true, StatementType.PREPARED);
		verify(delegate, times(1)).analyze("foo", true, StatementType.STATEMENT);
		verify(delegate, times(1)).analyze("foo", false, StatementType.STATEMENT);
		verify(delegate, times(1)).analyze("foo", true, StatementType.PREPARED);
		assertThat(result).isSameAs(foo3);
	}

	@Test
	void delegate() {
		OpenTelemetryQueryAnalyzer delegate = mock(OpenTelemetryQueryAnalyzer.class);
		OpenTelemetryQueryAnalyzerCache cache = new OpenTelemetryQueryAnalyzerCache(delegate, 1);
		assertThat(cache.getDelegate()).isSameAs(delegate);
	}

}
