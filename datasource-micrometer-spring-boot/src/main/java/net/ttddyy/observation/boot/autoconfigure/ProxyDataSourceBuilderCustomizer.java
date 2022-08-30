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

package net.ttddyy.observation.boot.autoconfigure;

import javax.sql.DataSource;

import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

/**
 * A customizer for {@link ProxyDataSourceBuilder}.
 *
 * @author Tadaya Tsuyukubo
 */
public interface ProxyDataSourceBuilderCustomizer {

	/**
	 * A callback to customize the given {@link ProxyDataSourceBuilder}.
	 * @param builder builder to customize
	 * @param dataSource datasource to proxy
	 * @param beanName bean name for the datasource
	 * @param dataSourceName resolved datasource name
	 */
	void customize(ProxyDataSourceBuilder builder, DataSource dataSource, String beanName, String dataSourceName);

}
