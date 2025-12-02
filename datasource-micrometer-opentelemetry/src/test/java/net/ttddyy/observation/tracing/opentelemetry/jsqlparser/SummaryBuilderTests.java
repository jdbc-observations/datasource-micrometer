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

package net.ttddyy.observation.tracing.opentelemetry.jsqlparser;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SummaryBuilder}.
 */
class SummaryBuilderTests {

	@Test
	void summary() {
		List<VisitedEntry> list = new ArrayList<>();
		list.add(VisitedEntry.operation("SELECT"));
		list.add(VisitedEntry.collection("emp"));

		SummaryBuilder builder = new SummaryBuilder();
		String summary = builder.build(list);
		assertThat(summary).isEqualTo("SELECT emp");
	}

	@Test
	void longName() {
		List<VisitedEntry> entries = new ArrayList<>();
		for (int i = 0; i < 30; i++) { // more than 255 chars
			entries.add(VisitedEntry.operation("SELECT"));
			entries.add(VisitedEntry.collection("emp"));
		}

		List<String> list = new ArrayList<>();
		for (int i = 0; i < 23; i++) {
			list.add("SELECT emp");
		}
		String expected = String.join(" ", list); // 252 char

		SummaryBuilder builder = new SummaryBuilder();
		String summary = builder.build(entries);
		assertThat(summary).isEqualTo(expected);
	}

}
