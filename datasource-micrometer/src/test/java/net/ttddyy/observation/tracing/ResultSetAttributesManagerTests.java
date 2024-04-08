/*
 * Copyright 2024 the original author or authors.
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

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;

import net.ttddyy.observation.tracing.ConnectionAttributesManager.ResultSetAttributes;
import net.ttddyy.observation.tracing.ConnectionAttributesManager.ResultSetAttributesManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Tadaya Tsuyukubo
 */
class ResultSetAttributesManagerTests {

	ResultSetAttributesManager manager = new ResultSetAttributesManager();

	@Test
	void removeByResultSet() {
		ResultSet resultSet1 = mock(ResultSet.class);
		ResultSet resultSet2 = mock(ResultSet.class);
		Statement statement = mock(Statement.class);
		ResultSetAttributes attributes1 = new ResultSetAttributes();
		ResultSetAttributes attributes2 = new ResultSetAttributes();

		this.manager.add(resultSet1, statement, attributes1);
		assertThat(this.manager.getByResultSet(resultSet1)).isSameAs(attributes1);
		assertThat(this.manager.getByResultSet(resultSet2)).isNull();

		this.manager.add(resultSet2, statement, attributes2);
		assertThat(this.manager.getByResultSet(resultSet1)).isSameAs(attributes1);
		assertThat(this.manager.getByResultSet(resultSet2)).isSameAs(attributes2);

		assertThat(this.manager.removeByResultSet(resultSet1)).isSameAs(attributes1);
		assertThat(this.manager.getByResultSet(resultSet1)).isNull();
		assertThat(this.manager.getByResultSet(resultSet2)).isSameAs(attributes2);
	}

	@Test
	void removeByStatement() {
		ResultSet resultSet1 = mock(ResultSet.class);
		ResultSet resultSet2 = mock(ResultSet.class);
		Statement statement = mock(Statement.class);
		ResultSetAttributes attributes1 = new ResultSetAttributes();
		ResultSetAttributes attributes2 = new ResultSetAttributes();

		this.manager.add(resultSet1, statement, attributes1);
		this.manager.add(resultSet2, statement, attributes2);
		assertThat(this.manager.getByResultSet(resultSet1)).isSameAs(attributes1);
		assertThat(this.manager.getByResultSet(resultSet2)).isSameAs(attributes2);

		Set<ResultSetAttributes> removed = this.manager.removeByStatement(statement);
		assertThat(removed).containsExactlyInAnyOrder(attributes1, attributes2);

		assertThat(this.manager.getByResultSet(resultSet1)).isNull();
		assertThat(this.manager.getByResultSet(resultSet2)).isNull();
	}

	@Test
	void isGeneratedKeys() {
		ResultSet generatedKeys = mock(ResultSet.class);
		ResultSet resultSet = mock(ResultSet.class);

		this.manager.addGeneratedKeys(generatedKeys);
		assertThat(this.manager.isGeneratedKeys(generatedKeys)).isTrue();
		assertThat(this.manager.isGeneratedKeys(resultSet)).isFalse();

		this.manager.removeAll();
		assertThat(this.manager.isGeneratedKeys(generatedKeys)).isFalse();
	}

	@Test
	void generatedKeysRemoval() {
		ResultSet generatedKeys = mock(ResultSet.class);
		Statement statement = mock(Statement.class);
		ResultSetAttributes attributes = new ResultSetAttributes();

		this.manager.addGeneratedKeys(generatedKeys);
		this.manager.add(generatedKeys, statement, attributes);
		assertThat(this.manager.isGeneratedKeys(generatedKeys)).isTrue();

		this.manager.removeByResultSet(generatedKeys);
		assertThat(this.manager.isGeneratedKeys(generatedKeys)).isFalse();
	}

}
