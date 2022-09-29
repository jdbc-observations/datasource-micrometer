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

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import net.ttddyy.dsproxy.ConnectionInfo;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.StatementType;
import net.ttddyy.dsproxy.proxy.ParameterSetOperation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultQueryParametersSpanTagProvider}.
 *
 * @author Tadaya Tsuyukubo
 */
class DefaultQueryParametersSpanTagProviderTests {

	@Test
	void preparedStatement() throws Exception {
		DefaultQueryParametersSpanTagProvider provider = new DefaultQueryParametersSpanTagProvider();

		ExecutionInfo executionInfo = new ExecutionInfo();
		executionInfo.setStatementType(StatementType.PREPARED);

		Method setStringMethod = PreparedStatement.class.getMethod("setString", int.class, String.class);

		List<ParameterSetOperation> firstParams = new ArrayList<>();
		firstParams.add(new ParameterSetOperation(setStringMethod, new Object[] { 0, "foo" }));
		firstParams.add(new ParameterSetOperation(setStringMethod, new Object[] { 1, "bar" }));

		List<ParameterSetOperation> secondParams = new ArrayList<>();
		secondParams.add(new ParameterSetOperation(setStringMethod, new Object[] { 0, "FOO" }));
		secondParams.add(new ParameterSetOperation(setStringMethod, new Object[] { 1, "BAR" }));

		List<List<ParameterSetOperation>> params = new ArrayList<>();
		params.add(firstParams);
		params.add(secondParams);

		QueryInfo queryInfo = new QueryInfo();
		queryInfo.setParametersList(params);
		List<QueryInfo> queryInfoList = new ArrayList<>();
		queryInfoList.add(queryInfo);

		String result = provider.getParameters(executionInfo, queryInfoList);
		assertThat(result).isEqualTo("(foo,bar),(FOO,BAR)");
	}

	@Test
	void callableStatement() throws Exception {
		DefaultQueryParametersSpanTagProvider provider = new DefaultQueryParametersSpanTagProvider();

		ExecutionInfo executionInfo = new ExecutionInfo();
		executionInfo.setStatementType(StatementType.CALLABLE);

		Method setStringMethod = CallableStatement.class.getMethod("setString", String.class, String.class);

		List<ParameterSetOperation> firstParams = new ArrayList<>();
		firstParams.add(new ParameterSetOperation(setStringMethod, new Object[] { "foo-key", "foo" }));
		firstParams.add(new ParameterSetOperation(setStringMethod, new Object[] { "bar-key", "bar" }));

		List<ParameterSetOperation> secondParams = new ArrayList<>();
		secondParams.add(new ParameterSetOperation(setStringMethod, new Object[] { "FOO-key", "FOO" }));
		secondParams.add(new ParameterSetOperation(setStringMethod, new Object[] { "BAR-key", "BAR" }));

		List<List<ParameterSetOperation>> params = new ArrayList<>();
		params.add(firstParams);
		params.add(secondParams);

		QueryInfo queryInfo = new QueryInfo();
		queryInfo.setParametersList(params);
		List<QueryInfo> queryInfoList = new ArrayList<>();
		queryInfoList.add(queryInfo);

		String result = provider.getParameters(executionInfo, queryInfoList);
		assertThat(result).isEqualTo("(bar-key=bar,foo-key=foo),(BAR-key=BAR,FOO-key=FOO)");
	}

	@Test
	void withNoParam() {
		DefaultQueryParametersSpanTagProvider provider = new DefaultQueryParametersSpanTagProvider();

		ExecutionInfo executionInfo = new ExecutionInfo();
		executionInfo.setStatementType(StatementType.PREPARED);

		// empty params
		ArrayList<List<ParameterSetOperation>> params = new ArrayList<>();

		QueryInfo queryInfo = new QueryInfo();
		queryInfo.setParametersList(params);
		List<QueryInfo> queryInfoList = new ArrayList<>();
		queryInfoList.add(queryInfo);

		String result = provider.getParameters(executionInfo, queryInfoList);
		assertThat(result).isEqualTo("");
	}

}
