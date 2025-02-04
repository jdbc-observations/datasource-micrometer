package net.ttddyy.observation.tracing;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.ObservationConvention;
import net.ttddyy.observation.tracing.JdbcObservationDocumentation.CommonLowCardinalityKeyNames;

import java.util.Optional;

public interface BaseObservationConvention<T extends DataSourceBaseContext> extends ObservationConvention<T> {

	default Optional<KeyValue> getDatasourceName(DataSourceBaseContext context) {
		return Optional.ofNullable(context.getDataSourceName())
				.map(name -> KeyValue.of(CommonLowCardinalityKeyNames.NAME, name));
	}

}
