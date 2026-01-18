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

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for OpenTelemetry Semantic Conventions support.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.3.0
 */
@ConfigurationProperties("jdbc.opentelemetry")
public class JdbcOpenTelemetryProperties {

	private final Attributes attributes = new Attributes();

	private final Analysis analysis = new Analysis();

	public Attributes getAttributes() {
		return this.attributes;
	}

	public Analysis getAnalysis() {
		return this.analysis;
	}

	public static class Attributes {

		/**
		 * Overrides any matching attributes.
		 */
		private Map<String, String> overrides = new HashMap<>();

		public Map<String, String> getOverrides() {
			return this.overrides;
		}

		public void setOverrides(Map<String, String> overrides) {
			this.overrides = overrides;
		}

	}

	public static class Analysis {

		/**
		 * Whether to perform query analysis.
		 */
		private boolean enabled = true;

		private final Summary summary = new Summary();

		private final Sanitize sanitize = new Sanitize();

		private final AnalysisCache cache = new AnalysisCache();

		public Summary getSummary() {
			return this.summary;
		}

		public Sanitize getSanitize() {
			return this.sanitize;
		}

		public AnalysisCache getCache() {
			return this.cache;
		}

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	public static class Summary {

		/**
		 * Whether to summarize queries for "db.query.summary".
		 */
		private boolean enabled = true;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	public static class Sanitize {

		/**
		 * Whether to sanitize queries for "db.query.text".
		 */
		private boolean enabled = true;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	public static class AnalysisCache {

		/**
		 * Whether to enable cache for query analysis.
		 */
		private boolean enabled = true;

		/**
		 * Cache size for query analysis.
		 */
		private int maxSize = 1000;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public int getMaxSize() {
			return this.maxSize;
		}

		public void setMaxSize(int maxSize) {
			this.maxSize = maxSize;
		}

	}

}
