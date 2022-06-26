package net.ttddyy.observation.tracing;

import java.net.URI;

import io.micrometer.common.lang.Nullable;
import io.micrometer.observation.Observation;

/**
 * @author Tadaya Tsuyukubo
 */
public class DataSourceBaseContext extends Observation.Context {

	private String dataSourceName;

	private URI url;


	public @Nullable String getDataSourceName() {
		return this.dataSourceName;
	}

	public void setDataSourceName(@Nullable String dataSourceName) {
		this.dataSourceName = dataSourceName;
	}

	public @Nullable URI getUrl() {
		return this.url;
	}

	public void setUrl(@Nullable URI url) {
		this.url = url;
	}

}
