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

package net.ttddyy.observation.boot.autoconfigure;

import javax.sql.DataSource;

/**
 * Strategy used to determine whether an eligible {@link DataSource} bean should be
 * wrapped with a proxy.
 * <p>
 * Applications can provide their own implementation to add custom logic to the proxy
 * creation process, for example based on the data source instance or bean name.
 * Framework-level checks, such as excluded bean names and scoped target detection, are
 * performed before this strategy is invoked.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.5.0
 */
@FunctionalInterface
public interface DataSourceProxyCreationStrategy {

	/**
	 * The default strategy, which creates a proxy for every eligible data source.
	 */
	DataSourceProxyCreationStrategy DEFAULT = (dataSource, beanName) -> true;

	/**
	 * Determines whether a proxy should be created for the given data source.
	 * @param dataSource the data source bean
	 * @param beanName the name of the data source bean
	 * @return {@code true} to create a proxy, or {@code false} otherwise
	 */
	boolean shouldCreateProxy(DataSource dataSource, String beanName);

}
