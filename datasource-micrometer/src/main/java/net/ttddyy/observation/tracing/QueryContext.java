package net.ttddyy.observation.tracing;

import java.net.URI;

import io.micrometer.common.lang.Nullable;
import io.micrometer.observation.Observation;

/**
 * @author Tadaya Tsuyukubo
 */
public final class QueryContext extends Observation.Context {

	// TODO: share the logic with ConnectionContext
	private String dataSourceName;

	private URI url;


	public String getDataSourceName() {
		return this.dataSourceName;
	}

	public void setDataSourceName(String dataSourceName) {
		this.dataSourceName = dataSourceName;
	}

	@Nullable
	public URI getUrl() {
		return this.url;
	}

	public void setUrl(@Nullable URI url) {
		this.url = url;
	}
}
