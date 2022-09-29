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

import java.util.List;
import java.util.SortedMap;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.StatementType;
import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;
import net.ttddyy.dsproxy.proxy.ParameterSetOperation;

/**
 * Default implementation for {@link QueryParametersSpanTagProvider} leveraging methods
 * provided by {@link DefaultQueryLogEntryCreator}.
 *
 * @author Tadaya Tsuyukubo
 */
public class DefaultQueryParametersSpanTagProvider extends DefaultQueryLogEntryCreator
		implements QueryParametersSpanTagProvider {

	@Override
	public String getParameters(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
		boolean isPrepared = execInfo.getStatementType() == StatementType.PREPARED;
		StringBuilder sb = new StringBuilder();

		// copied from "DefaultQueryLogEntryCreator#writeParamsEntry"
		for (QueryInfo queryInfo : queryInfoList) {
			for (List<ParameterSetOperation> parameters : queryInfo.getParametersList()) {
				SortedMap<String, String> paramMap = getParametersToDisplay(parameters);

				// parameters per batch.
				// for prepared: (val1,val2,...)
				// for callable: (key1=val1,key2=val2,...)
				if (isPrepared) {
					writeParamsForSinglePreparedEntry(sb, paramMap, execInfo, queryInfoList);
				}
				else {
					writeParamsForSingleCallableEntry(sb, paramMap, execInfo, queryInfoList);
				}
			}
		}
		// Until this method works with empty string, check the length.
		// https://github.com/jdbc-observations/datasource-proxy/issues/86
		if (sb.length() > 0) {
			chompIfEndWith(sb, ',');
		}
		return sb.toString();
	}

}
