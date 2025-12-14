/*
 * Copyright 2025 the original author or authors.
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

package net.ttddyy.observation.tracing.opentelemetry.jsqlparser;

import io.micrometer.common.util.internal.logging.InternalLogger;
import io.micrometer.common.util.internal.logging.InternalLoggerFactory;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Block;
import net.sf.jsqlparser.statement.Commit;
import net.sf.jsqlparser.statement.DeclareStatement;
import net.sf.jsqlparser.statement.DescribeStatement;
import net.sf.jsqlparser.statement.ExplainStatement;
import net.sf.jsqlparser.statement.IfElseStatement;
import net.sf.jsqlparser.statement.PurgeStatement;
import net.sf.jsqlparser.statement.ResetStatement;
import net.sf.jsqlparser.statement.RollbackStatement;
import net.sf.jsqlparser.statement.SavepointStatement;
import net.sf.jsqlparser.statement.SetStatement;
import net.sf.jsqlparser.statement.ShowColumnsStatement;
import net.sf.jsqlparser.statement.ShowStatement;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.UnsupportedStatement;
import net.sf.jsqlparser.statement.UseStatement;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.alter.AlterSession;
import net.sf.jsqlparser.statement.alter.RenameTableStatement;
import net.sf.jsqlparser.statement.alter.sequence.AlterSequence;
import net.sf.jsqlparser.statement.analyze.Analyze;
import net.sf.jsqlparser.statement.comment.Comment;
import net.sf.jsqlparser.statement.create.function.CreateFunction;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.procedure.CreateProcedure;
import net.sf.jsqlparser.statement.create.schema.CreateSchema;
import net.sf.jsqlparser.statement.create.sequence.CreateSequence;
import net.sf.jsqlparser.statement.create.synonym.CreateSynonym;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.AlterView;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.execute.Execute;
import net.sf.jsqlparser.statement.grant.Grant;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.refresh.RefreshMaterializedViewStatement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.show.ShowIndexStatement;
import net.sf.jsqlparser.statement.show.ShowTablesStatement;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.upsert.Upsert;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import net.ttddyy.dsproxy.StatementType;
import net.ttddyy.observation.tracing.opentelemetry.OpenTelemetryQueryAnalyzer;
import net.ttddyy.observation.tracing.opentelemetry.QueryAnalysisResult;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * {@link OpenTelemetryQueryAnalyzer} implementation with JSqlParser.
 *
 * @since 1.3.0
 */
public class JSqlParserQueryAnalyzer implements OpenTelemetryQueryAnalyzer {

	private static final InternalLogger logger = InternalLoggerFactory.getInstance(JSqlParserQueryAnalyzer.class);

	private Consumer<CCJSqlParser> parserConfigurer = (parser) -> {
	};

	private ExecutorService executorService = Executors.newSingleThreadExecutor();

	private boolean sanitizeEnabled = true;

	private boolean summaryEnabled = true;

	@Override
	public QueryAnalysisResult analyze(String query, boolean isBatch, StatementType statementType) {
		QueryAnalysisResult result = new QueryAnalysisResult();

		if (statementType == StatementType.PREPARED) {
			// query-text is only populated with sanitized statement or prepared.
			// populate query-text before parsing it to include the text in result when
			// parsing failed.
			result.setQueryText(query);
		}
		if (statementType == StatementType.CALLABLE) {
			// remove '{' and '}'
			query = query.trim().replaceAll("^\\{|\\}$", "");
		}

		Statement statement;
		try {
			statement = CCJSqlParserUtil.parse(query, this.executorService, this.parserConfigurer);
		}
		catch (JSQLParserException ex) {
			logger.debug("Failed to parse query {}", query, ex);
			return result;
		}
		String operation = getOperation(statement);
		String operationName = isBatch ? "BATCH " + operation : operation;
		result.setOperationName(operationName);

		JSqlParserQueryVisitedContext visited = visitStatement(statement);
		List<VisitedEntry> entries = visited.getEntries();
		String mainTableName = visited.getMainTableName();

		if (this.summaryEnabled) {
			String summary = new QuerySummaryBuilder().build(entries);
			result.setQuerySummary(summary);
		}

		if (statementType == StatementType.STATEMENT && this.sanitizeEnabled) {
			String sanitizedQuery = getSanitizedQuery(statement);
			result.setQueryText(sanitizedQuery);
		}

		if (statementType == StatementType.CALLABLE) {
			result.setStoredProcedureName(mainTableName);
		}
		else {
			result.setCollectionName(mainTableName);
		}

		return result;
	}

	protected JSqlParserQueryVisitedContext visitStatement(Statement statement) {
		JSqlParserQueryVisitor visitor = new JSqlParserQueryVisitor();
		return visitor.visit(statement);
	}

	protected String getSanitizedQuery(Statement statement) {
		StringBuilder sb = new StringBuilder();
		StatementDeParser deParser = new StatementDeParser(new JSqlParserSanitizingExpressionDeParser(),
				new SelectDeParser(), sb);
		statement.accept(deParser);

		// TODO: truncation?
		return sb.toString();
	}

	protected String getOperation(Statement stmt) {
		// parent class first
		if (stmt instanceof Select) {
			return "SELECT";
		}
		if (stmt instanceof Insert) {
			return "INSERT";
		}
		if (stmt instanceof Delete) {
			return "DELETE";
		}
		if (stmt instanceof Update) {
			return "UPDATE";
		}

		// for stored procedure
		if (stmt instanceof Execute) {
			return ((Execute) stmt).getExecType().toString();
		}

		if (stmt instanceof Alter) {
			return "ALTER";
		}
		if (stmt instanceof AlterView) {
			return "ALTER_VIEW";
		}
		if (stmt instanceof AlterSession) {
			return "ALTER_SESSION";
		}
		if (stmt instanceof AlterSequence) {
			return "ALTER_SEQUENCE";
		}
		if (stmt instanceof AlterExpression) {
			return "ALTER_EXPRESSION";
		}
		if (stmt instanceof Analyze) {
			return "ANALYZE";
		}
		if (stmt instanceof Block) {
			return "BLOCK";
		}
		if (stmt instanceof Comment) {
			return "COMMENT";
		}
		if (stmt instanceof Commit) {
			return "COMMIT";
		}
		if (stmt instanceof CreateProcedure) {
			return "CREATE_PROCEDURE";
		}
		if (stmt instanceof CreateFunction) {
			return "CREATE_FUNCTION";
		}
		if (stmt instanceof CreateIndex) {
			return "CREATE_INDEX";
		}
		if (stmt instanceof CreateSchema) {
			return "CREATE_SCHEMA";
		}
		if (stmt instanceof CreateSequence) {
			return "CREATE_SEQUENCE";
		}
		if (stmt instanceof CreateSynonym) {
			return "CREATE_SYNONYM";
		}
		if (stmt instanceof CreateTable) {
			return "CREATE_TABLE";
		}
		if (stmt instanceof CreateView) {
			return "CREATE_VIEW";
		}
		if (stmt instanceof DeclareStatement) {
			return "DECLARE";
		}
		if (stmt instanceof DescribeStatement) {
			return "DESCRIBE";
		}
		if (stmt instanceof Drop) {
			return "DROP";
		}
		if (stmt instanceof ExplainStatement) {
			return "EXPLAIN";
		}
		if (stmt instanceof Grant) {
			return "GRANT";
		}
		if (stmt instanceof IfElseStatement) {
			return "IF";
		}
		if (stmt instanceof Merge) {
			return "MERGE";
		}
		if (stmt instanceof PurgeStatement) {
			return "PURGE";
		}
		if (stmt instanceof RefreshMaterializedViewStatement) {
			return "REFRESH_MATERIALIZED_VIEW";
		}
		if (stmt instanceof RenameTableStatement) {
			return "RENAME_TABLE";
		}
		if (stmt instanceof ResetStatement) {
			return "RESET";
		}
		if (stmt instanceof RollbackStatement) {
			return "ROLLBACK";
		}
		if (stmt instanceof SavepointStatement) {
			return "SAVEPOINT";
		}
		if (stmt instanceof SetStatement) {
			return "SET";
		}
		if (stmt instanceof ShowColumnsStatement) {
			return "SHOW_COLUMNS";
		}
		if (stmt instanceof ShowIndexStatement) {
			return "SHOW_INDEX";
		}
		if (stmt instanceof ShowStatement) {
			return "SHOW";
		}
		if (stmt instanceof ShowTablesStatement) {
			return "SHOW_TABLES";
		}
		if (stmt instanceof Truncate) {
			return "TRUNCATE";
		}
		if (stmt instanceof UnsupportedStatement) {
			return "UNSUPPORTED";
		}
		if (stmt instanceof Upsert) {
			return "UPSERT";
		}
		if (stmt instanceof UseStatement) {
			return "USE";
		}

		return "UNKNOWN";
	}

	public void setParserConfigurer(Consumer<CCJSqlParser> parserConfigurer) {
		this.parserConfigurer = parserConfigurer;
	}

	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}

	public void setSanitizeEnabled(boolean sanitizeEnabled) {
		this.sanitizeEnabled = sanitizeEnabled;
	}

	public void setSummaryEnabled(boolean summaryEnabled) {
		this.summaryEnabled = summaryEnabled;
	}
}
