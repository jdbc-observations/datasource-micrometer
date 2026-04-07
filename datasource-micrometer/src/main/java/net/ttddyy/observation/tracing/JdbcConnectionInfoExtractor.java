/*
 * Copyright 2026 the original author or authors.
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

package net.ttddyy.observation.tracing;

import io.micrometer.common.lang.Nullable;

import java.sql.Connection;
import java.util.Objects;

/**
 * Retrieve necessary details from {@link Connection}.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.4.1
 */
public interface JdbcConnectionInfoExtractor {

	JdbcConnectionInfo EMPTY_RESULT = new JdbcConnectionInfo(null, null, 0);

	JdbcConnectionInfo extract(Connection connection);

	class JdbcConnectionInfo {

		@Nullable
		private final String url;

		@Nullable
		private final String host;

		private final int port;

		public JdbcConnectionInfo(@Nullable String url, @Nullable String host, int port) {
			this.url = url;
			this.host = host;
			this.port = port;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass())
				return false;

			JdbcConnectionInfo that = (JdbcConnectionInfo) o;
			return port == that.port && Objects.equals(url, that.url) && Objects.equals(host, that.host);
		}

		@Override
		public int hashCode() {
			int result = Objects.hashCode(url);
			result = 31 * result + Objects.hashCode(host);
			result = 31 * result + port;
			return result;
		}

		public @Nullable String getUrl() {
			return this.url;
		}

		public @Nullable String getHost() {
			return this.host;
		}

		public int getPort() {
			return this.port;
		}

	}

}
