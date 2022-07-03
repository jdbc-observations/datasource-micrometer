package net.ttddyy.observation.boot.autoconfigure;

import javax.sql.DataSource;

/**
 * Resolve {@link DataSource} name.
 *
 * @author Tadaya Tsuyukubo
 */
public interface DataSourceNameResolver {

	String resolve(String beanName, DataSource dataSource);

}
