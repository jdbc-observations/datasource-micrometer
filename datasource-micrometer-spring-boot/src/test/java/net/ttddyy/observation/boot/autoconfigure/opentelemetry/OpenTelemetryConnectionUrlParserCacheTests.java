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

import net.ttddyy.observation.tracing.opentelemetry.OpenTelemetryConnectionUrlParser;
import net.ttddyy.observation.tracing.opentelemetry.UrlParseResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link OpenTelemetryConnectionUrlParserCache}.
 */
class OpenTelemetryConnectionUrlParserCacheTests {

	@Test
	void cache() {
		OpenTelemetryConnectionUrlParser delegate = mock(OpenTelemetryConnectionUrlParser.class);
		UrlParseResult fooResult = mock(UrlParseResult.class);
		UrlParseResult barResult = mock(UrlParseResult.class);
		given(delegate.parse("/foo")).willReturn(fooResult);
		given(delegate.parse("/bar")).willReturn(barResult);

		OpenTelemetryConnectionUrlParserCache cache = new OpenTelemetryConnectionUrlParserCache(delegate);

		UrlParseResult result;
		result = cache.parse("/foo");
		assertThat(result).isSameAs(fooResult);
		verify(delegate, times(1)).parse("/foo");
		verify(delegate, times(0)).parse("/bar");

		result = cache.parse("/foo");
		assertThat(result).isSameAs(fooResult);
		verify(delegate, times(1)).parse("/foo");
		verify(delegate, times(0)).parse("/bar");

		result = cache.parse("/bar");
		assertThat(result).isSameAs(barResult);
		verify(delegate, times(1)).parse("/foo");
		verify(delegate, times(1)).parse("/bar");
	}

	@Test
	void delegate() {
		OpenTelemetryConnectionUrlParser delegate = mock(OpenTelemetryConnectionUrlParser.class);
		OpenTelemetryConnectionUrlParserCache cache = new OpenTelemetryConnectionUrlParserCache(delegate);
		assertThat(cache.getDelegate()).isSameAs(delegate);
	}

}
