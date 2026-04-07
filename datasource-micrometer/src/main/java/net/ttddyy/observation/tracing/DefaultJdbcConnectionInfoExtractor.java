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

import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link JdbcConnectionInfoExtractor}.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.4.1
 */
public class DefaultJdbcConnectionInfoExtractor implements JdbcConnectionInfoExtractor {

	private final Map<String, JdbcConnectionInfo> cache = new ConcurrentHashMap<>();

	@Override
	public JdbcConnectionInfo extract(Connection connection) {
		String jdbcUrl = retrieveUrl(connection);
		if (jdbcUrl == null) {
			return JdbcConnectionInfoExtractor.EMPTY_RESULT;
		}
		return this.cache.computeIfAbsent(jdbcUrl, key -> {
			URI url;
			try {
				// strip "jdbc:"
				String urlAsString = jdbcUrl.substring(5);
				// Remove all white space according to RFC 2396;
				url = URI.create(urlAsString.replace(" ", ""));
			}
			catch (Exception ex) {
				return JdbcConnectionInfoExtractor.EMPTY_RESULT;
			}
			return new JdbcConnectionInfo(key, url.getHost(), url.getPort());
		});
	}

	protected @Nullable String retrieveUrl(Connection connection) {
		try {
			return connection.getMetaData().getURL();
		}
		catch (SQLException ex) {
			return null;
		}
	}

	public void clear() {
		this.cache.clear();
	}

}
