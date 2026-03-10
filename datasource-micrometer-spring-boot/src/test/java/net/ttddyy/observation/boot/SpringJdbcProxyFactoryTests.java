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

package net.ttddyy.observation.boot;

import com.zaxxer.hikari.HikariDataSource;
import net.ttddyy.dsproxy.proxy.ProxyConfig;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.Assert;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringJdbcProxyFactory}
 *
 * @author Tadaya Tsuyukubo
 */
class SpringJdbcProxyFactoryTests {

	@Test
	void refresh() {
		// Simulate Spring Cloud refresh behavior. (gh-99)
		// see: ContextRefresher, EnvironmentChangeEvent, ConfigurationPropertiesRebinder
		HikariDataSource hikariDataSource = new HikariDataSource();
		SpringJdbcProxyFactory proxyFactory = new SpringJdbcProxyFactory();
		DataSource dataSource = proxyFactory.createDataSource(hikariDataSource, ProxyConfig.Builder.create().build());

		assertThat(AopUtils.isAopProxy(dataSource)).isTrue();
		assertThat((Object) getTargetObject(dataSource)).isSameAs(hikariDataSource);
	}

	// copied from Spring Cloud's "ProxyUtils#getTargetObject" to avoid dependency.
	// https://github.com/spring-cloud/spring-cloud-commons/blob/main/spring-cloud-context/src/main/java/org/springframework/cloud/util/ProxyUtils.java#L33-L47
	public static <T> T getTargetObject(Object candidate) {
		Assert.notNull(candidate, "Candidate must not be null");
		try {
			if (AopUtils.isAopProxy(candidate) && candidate instanceof Advised) {
				Object target = ((Advised) candidate).getTargetSource().getTarget();
				if (target != null) {
					return (T) getTargetObject(target);
				}
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to unwrap proxied object", ex);
		}
		return (T) candidate;
	}

}
