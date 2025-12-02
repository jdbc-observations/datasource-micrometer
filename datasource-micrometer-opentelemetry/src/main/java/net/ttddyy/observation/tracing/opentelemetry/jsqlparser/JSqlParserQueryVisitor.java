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

import io.micrometer.common.lang.Nullable;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.analyze.Analyze;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.execute.Execute;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.refresh.RefreshMaterializedViewStatement;
import net.sf.jsqlparser.statement.select.FromItemVisitor;
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.upsert.Upsert;

import java.util.ArrayList;
import java.util.List;

/**
 * Visitor implementation to retrieve a list of {@link VisitedEntry entries}.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.3.0
 */
public class JSqlParserQueryVisitor extends StatementVisitorAdapter<Void> {

	FromItemVisitor<Void> fromItemVisitor = new FromItemVisitorAdapter<Void>() {
		@Override
		public <S> Void visit(PlainSelect plainSelect, S context) {
			plainSelect.getFromItem().accept(this, context);
			visitJoins(plainSelect.getJoins(), context);
			return null;
		}

		private <S> void visitJoins(@Nullable List<Join> joins, S context) {
			if (joins == null) {
				return;
			}
			for (Join join : joins) {
				join.getFromItem().accept(this, context);
				join.getRightItem().accept(this, context);
				// joins may duplicate the collection names. dedupe them
				((VisitedContext) context).dedupeLastTwoEntries();
			}
			// for join, there is no main table
			clearMainTable(context);
		}

		@Override
		public <S> Void visit(ParenthesedSelect select, S context) {
			addOperation(context, "SELECT");
			return select.getSelect().accept(this, context);
		}

		@Override
		public <S> Void visit(Table table, S context) {
			addCollection(context, table.getName());
			// e.g:
			// 'aa bb' => aa bb
			// 'aa'.'bb' => `aa`.`bb`
			String tableName;
			String schema = table.getSchemaName();
			if (schema != null) {
				tableName = schema + "." + table.getName();
			}
			else {
				tableName = table.getUnquotedName();
			}
			setMainTableIfEmpty(context, tableName);
			return null;
		}
	};

	/**
	 * Entry point to this visitor.
	 * @param statement parsed statement
	 * @return list of visited entries
	 */
	public VisitedContext visit(Statement statement) {
		VisitedContext context = new VisitedContext();
		statement.accept(this, context);
		return context;
	}

	@Override
	public <S> Void visit(Select select, S context) {
		addOperation(context, "SELECT");
		return select.accept(this.fromItemVisitor, context);
	}

	@Override
	public <S> Void visit(Delete delete, S context) {
		addOperation(context, "DELETE");
		visitJoins(delete.getJoins(), context);
		return this.fromItemVisitor.visit(delete.getTable(), context);
	}

	@Override
	public <S> Void visit(Update update, S context) {
		addOperation(context, "UPDATE");
		return this.fromItemVisitor.visit(update.getTable(), context);
	}

	@Override
	public <S> Void visit(Insert insert, S context) {
		addOperation(context, "INSERT");
		this.fromItemVisitor.visit(insert.getTable(), context);
		if (insert.getSelect() != null) {
			visit(insert.getSelect(), context);
		}
		return null;
	}

	@Override
	public <S> Void visit(Analyze analyze, S context) {
		addOperation(context, "ANALYZE");
		return this.fromItemVisitor.visit(analyze.getTable(), context);
	}

	@Override
	public <S> Void visit(Drop drop, S context) {
		addOperation(context, "DROP");
		return this.fromItemVisitor.visit(drop.getName(), context);
	}

	@Override
	public <S> Void visit(Truncate truncate, S context) {
		addOperation(context, "TRUNCATE");
		return this.fromItemVisitor.visit(truncate.getTable(), context);
	}

	@Override
	public <S> Void visit(CreateTable createTable, S context) {
		addOperation(context, "CREATE TABLE");
		return this.fromItemVisitor.visit(createTable.getTable(), context);
	}

	@Override
	public <S> Void visit(CreateView createView, S context) {
		addOperation(context, "CREATE VIEW");
		return this.fromItemVisitor.visit(createView.getView(), context);
	}

	@Override
	public <S> Void visit(Merge merge, S context) {
		addOperation(context, "MERGE");
		return this.fromItemVisitor.visit(merge.getTable(), context);
	}

	@Override
	public <S> Void visit(RefreshMaterializedViewStatement materializedView, S context) {
		addOperation(context, "REFRESH MATERIALIZED VIEW");
		return this.fromItemVisitor.visit(materializedView.getView(), context);
	}

	@Override
	public <S> Void visit(Upsert upsert, S context) {
		addOperation(context, "UPSERT");
		return this.fromItemVisitor.visit(upsert.getTable(), context);
	}

	@Override
	public <S> Void visit(Execute execute, S context) {
		addOperation(context, execute.getExecType().toString());
		if (execute.getExprList() != null) {

		}
		addCollection(context, execute.getName());
		setMainTableIfEmpty(context, execute.getName());
		return null;
	}

	// TODO: combine
	private <S> void visitJoins(@Nullable List<Join> joins, S context) {
		if (joins == null) {
			return;
		}
		for (Join join : joins) {
			join.getFromItem().accept(this.fromItemVisitor, context);
			join.getRightItem().accept(this.fromItemVisitor, context);
			// joins may duplicate the collection names. dedupe them
			((VisitedContext) context).dedupeLastTwoEntries();
		}
	}

	private <S> void addOperation(S context, String operationName) {
		((VisitedContext) context).addOperation(operationName);
	}

	private <S> void addCollection(S context, String collectionName) {
		((VisitedContext) context).addCollection(collectionName);
	}

	private <S> void setMainTableIfEmpty(S context, String collectionName) {
		VisitedContext visitedContext = (VisitedContext) context;
		if (visitedContext.getMainTableName() == null) {
			visitedContext.setMainTableName(collectionName);
		}
	}

	private <S> void clearMainTable(S context) {
		((VisitedContext) context).setMainTableName(null);
	}

	static class VisitedContext {

		private final List<VisitedEntry> entries = new ArrayList<>();

		@Nullable
		private String mainTableName;

		public List<VisitedEntry> getEntries() {
			return this.entries;
		}

		public void addOperation(String operationName) {
			this.entries.add(VisitedEntry.operation(operationName));
		}

		public void addCollection(String collectionName) {
			this.entries.add(VisitedEntry.collection(collectionName));
		}

		public void dedupeLastTwoEntries() {
			if (this.entries.size() < 2) {
				return;
			}
			VisitedEntry last = this.entries.remove(this.entries.size() - 1);
			VisitedEntry secondLast = this.entries.get(this.entries.size() - 1);
			if (!last.equals(secondLast)) {
				this.entries.add(last);
			}
		}

		/**
		 * Main table name for prepared statement. For callable statement, this becomes
		 * the name of procedure.
		 * @return name
		 */
		@Nullable
		public String getMainTableName() {
			return this.mainTableName;
		}

		public void setMainTableName(@Nullable String mainTableName) {
			this.mainTableName = mainTableName;
		}

	}

}
