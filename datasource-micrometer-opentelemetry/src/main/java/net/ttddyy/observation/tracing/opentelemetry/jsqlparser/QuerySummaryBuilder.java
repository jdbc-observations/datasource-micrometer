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

import java.util.List;

/**
 * Construct query summary.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.3.0
 */
public class QuerySummaryBuilder {

	private int max = 255;

	public String build(List<VisitedEntry> entries) {
		StringBuilder builder = new StringBuilder();
		for (VisitedEntry entry : entries) {
			String data = entry.getData();
			if (builder.length() + data.length() + 1 > this.max) {
				break;
			}
			if (builder.length() != 0) {
				builder.append(' ');
			}
			builder.append(data);
		}
		return builder.toString();
	}

	public void setMax(int max) {
		this.max = max;
	}

}
