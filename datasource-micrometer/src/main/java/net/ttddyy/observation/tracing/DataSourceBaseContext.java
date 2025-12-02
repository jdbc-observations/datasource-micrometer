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

import javax.sql.DataSource;

import io.micrometer.common.lang.Nullable;
import io.micrometer.observation.Observation;
import io.micrometer.observation.transport.Kind;
import io.micrometer.observation.transport.SenderContext;

/**
 * Base {@link Observation.Context Context} for datasource observation context classes.
 *
 * @author Tadaya Tsuyukubo
 */
public class DataSourceBaseContext extends SenderContext<Object> {

	public DataSourceBaseContext() {
		super((carrier, key, value) -> {
			// no-op setter
		}, Kind.CLIENT);
	}

	private DataSource dataSource;

	private String dataSourceName;

	@Nullable
	private String url;

	@Nullable
	private String host;

	private int port;

	public DataSource getDataSource() {
		return this.dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Nullable
	public String getDataSourceName() {
		return this.dataSourceName;
	}

	public void setDataSourceName(@Nullable String dataSourceName) {
		this.dataSourceName = dataSourceName;
	}

	@Nullable
	public String getUrl() {
		return this.url;
	}

	public void setUrl(@Nullable String url) {
		this.url = url;
	}

	@Nullable
	public String getHost() {
		return this.host;
	}

	public void setHost(@Nullable String host) {
		this.host = host;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
