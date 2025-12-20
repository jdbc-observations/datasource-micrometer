package net.ttddyy.observation.tracing.opentelemetry.jsqlparser;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Tests for {@link JSqlParserQueryVisitor}.
 */
class JSqlParserQueryVisitorTests {

	@Test
	void simpleSelect() throws Exception {
		String query = "select * from emp;";
		Statement statement = CCJSqlParserUtil.parse(query);

		JSqlParserQueryVisitor visitor = new JSqlParserQueryVisitor();
		JSqlParserQueryVisitedContext visited = visitor.visit(statement);
		List<VisitedEntry> entries = visited.getEntries();
		String mainTableName = visited.getMainTableName();

		assertThat(entries).containsExactly(VisitedEntry.operation("SELECT"), VisitedEntry.collection("emp"));
		assertThat(mainTableName).isEqualTo("emp");
	}

	@Test
	void querySelect() throws Exception {
		String query = "select * from (SELECT * FROM emp);";
		Statement statement = CCJSqlParserUtil.parse(query);

		JSqlParserQueryVisitor visitor = new JSqlParserQueryVisitor();
		JSqlParserQueryVisitedContext visited = visitor.visit(statement);
		List<VisitedEntry> entries = visited.getEntries();
		String mainTableName = visited.getMainTableName();

		assertThat(entries).containsExactly(VisitedEntry.operation("SELECT"), VisitedEntry.operation("SELECT"),
				VisitedEntry.collection("emp"));
		assertThat(mainTableName).isEqualTo("emp");
	}

	@Test
	void insertSelect() throws Exception {
		String query = "INSERT INTO shipping_details (order_id, address) SELECT order_id, address FROM orders"
				+ " WHERE  order_id = ?";
		Statement statement = CCJSqlParserUtil.parse(query);

		JSqlParserQueryVisitor visitor = new JSqlParserQueryVisitor();
		JSqlParserQueryVisitedContext visited = visitor.visit(statement);
		List<VisitedEntry> entries = visited.getEntries();
		String mainTableName = visited.getMainTableName();

		assertThat(entries).containsExactly(VisitedEntry.operation("INSERT"),
				VisitedEntry.collection("shipping_details"), VisitedEntry.operation("SELECT"),
				VisitedEntry.collection("orders"));
		assertThat(mainTableName).isEqualTo("shipping_details");
	}

	@Test
	void multipleTables() throws Exception {
		String query = "SELECT * FROM songs, artists WHERE songs.artist_id = artists.id";
		Statement statement = CCJSqlParserUtil.parse(query);

		JSqlParserQueryVisitor visitor = new JSqlParserQueryVisitor();
		JSqlParserQueryVisitedContext visited = visitor.visit(statement);
		List<VisitedEntry> entries = visited.getEntries();
		String mainTableName = visited.getMainTableName();

		assertThat(entries).containsExactly(VisitedEntry.operation("SELECT"), VisitedEntry.collection("songs"),
				VisitedEntry.collection("artists"));
		assertThat(mainTableName).isNull();
	}

	@Test
	void operationOnAnonymousTable() throws Exception {
		String query = "SELECT order_date FROM (SELECT * FROM orders o JOIN customers c ON o.customer_id = c.customer_id)";
		Statement statement = CCJSqlParserUtil.parse(query);

		JSqlParserQueryVisitor visitor = new JSqlParserQueryVisitor();
		JSqlParserQueryVisitedContext visited = visitor.visit(statement);
		List<VisitedEntry> entries = visited.getEntries();
		String mainTableName = visited.getMainTableName();

		assertThat(entries).containsExactly(VisitedEntry.operation("SELECT"), VisitedEntry.operation("SELECT"),
				VisitedEntry.collection("orders"), VisitedEntry.collection("customers"));
		assertThat(mainTableName).isNull();
	}

	@Test
	void multipleCollectionsWithDoubleQuotesOrOtherPunctuation() throws Exception {
		String query = "SELECT * FROM \"song list\", 'artists'";
		Statement statement = CCJSqlParserUtil.parse(query);

		JSqlParserQueryVisitor visitor = new JSqlParserQueryVisitor();
		JSqlParserQueryVisitedContext visited = visitor.visit(statement);
		List<VisitedEntry> entries = visited.getEntries();
		String mainTableName = visited.getMainTableName();

		assertThat(entries).containsExactly(VisitedEntry.operation("SELECT"), VisitedEntry.collection("\"song list\""),
				VisitedEntry.collection("'artists'"));
		assertThat(mainTableName).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "call my_procedure(?,?,?)", "call my_procedure()", "call my_procedure" })
	void callable(String query) throws Exception {
		Statement statement = CCJSqlParserUtil.parse(query);

		JSqlParserQueryVisitor visitor = new JSqlParserQueryVisitor();
		JSqlParserQueryVisitedContext visited = visitor.visit(statement);
		List<VisitedEntry> entries = visited.getEntries();
		String mainTableName = visited.getMainTableName();

		assertThat(entries).containsExactly(VisitedEntry.operation("CALL"), VisitedEntry.collection("my_procedure"));
		assertThat(mainTableName).isEqualTo("my_procedure");
	}

	@ParameterizedTest
	@MethodSource
	void mainTableName(String query, String expected) throws Exception {
		Statement statement = CCJSqlParserUtil.parse(query);

		JSqlParserQueryVisitor visitor = new JSqlParserQueryVisitor();
		JSqlParserQueryVisitedContext visited = visitor.visit(statement);

		String mainTableName = visited.getMainTableName();
		assertThat(mainTableName).isEqualTo(expected);
	}

	static Stream<Arguments> mainTableName() {
		// many of data came from:
		// https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation-api-incubator/src/test/java/io/opentelemetry/instrumentation/api/incubator/semconv/db/SqlStatementSanitizerTest.java
		return Stream.of(
		// @formatter:off
			arguments("SELECT x, y, z FROM table", "table"),
			arguments("SELECT x, y, z FROM `table`", "table"),
			arguments("SELECT x, y, z FROM \"table\"", "table"),

			arguments("SELECT x, y, z FROM schema.table", "schema.table"),
			arguments("SELECT x, y, z FROM `schema table`", "schema table"),
			arguments("SELECT x, y, z FROM `schema`.`table`", "`schema`.`table`"),
			arguments("SELECT x, y, z FROM \"schema table\"", "schema table"),

			arguments("WITH subquery as (select a from b) SELECT x, y, z FROM table", "table"),  // different from otel
			arguments("SELECT x, y, (select a from b) as z FROM table", "table"),  //

			arguments("select col /* from table2 */ from table", "table"),
			arguments("select col from table join anotherTable", null),
			arguments("select col from (select * from anotherTable)", "anotherTable"), // different from otel
			arguments("select col from (select * from anotherTable) alias", "anotherTable"), // different from otel
			arguments("select col from table1 union select col from table2", null),
			arguments("select col from table where col in (select * from anotherTable)", "table"), // different from otel
			arguments("select col from table1, table2", null),
			arguments("select col from table1 t1, table2 t2", null),
			arguments("select col from table1 as t1, table2 as t2", null),
			arguments("select col from table where col in (1, 2, 3)", "table"),
			arguments("select 'a' IN(x, 'b') from table where col in (1) and z IN( '3', '4' )", "table"),
			arguments("select col from table order by col, col2", "table"),

			arguments("/* update comment */ select * from table", "table"),
			arguments("select /*((*/abc from table", "table"),
			arguments("SeLeCT * FrOm TAblE", "TAblE"), //

			arguments("select * /* update comment */ from table", "table"),

			arguments("insert into table1 (name) values ('foo')", "table1"),
			arguments("insert into schema.table1 (name) values ('foo')", "schema.table1"),
			arguments("insert into `schema`.`table1` (name) values ('foo')", "`schema`.`table1`"),
			arguments("insert into \"foo bar\" (name) values ('foo')", "foo bar"),

			arguments("delete from table1", "table1"),
			arguments("delete from table1 where id=1", "table1"),
			arguments("delete from `my table` where id=1", "my table"),
			arguments("delete from foo where x IN (1,2,3)", "foo"),
			arguments("delete from foo where x IN (?)", "foo"),

			arguments("update table set answer=42", "table"),
			arguments("update table set answer=?", "table"),
			arguments("update `my table` set answer=?", "my table"),
			arguments("update `my table` set answer=42 where x IN('a', 'b') AND y In ('a', 'b')", "my table"),
			arguments("update `my table` set answer=? where x IN(?) AND y In (?)", "my table"),
			arguments("update \"my table\" set answer=42", "my table"),

			arguments("call test_proc()", "test_proc"),
			arguments("call test_proc", "test_proc"),
			arguments("call db.test_proc", "db.test_proc"),

			arguments("merge into target as t using source as s on (t.id=s.id)", "target"),
			arguments("merge into `my target` as t using source as s on (t.id=s.id)", "my target"),
			arguments("merge into \"my target\" as t using source as s on (t.id=s.id)", "my target"),

			arguments("insert into target (name) select name from source", "target"),
			arguments("with cte (name) as (select name from source) select name from cte", "cte"),

			arguments("CREATE TABLE `table`", "table"),
			arguments("CREATE TABLE IF NOT EXISTS table", "table"),
			arguments("DROP TABLE `if`", "if"),
			arguments("ALTER TABLE table ADD CONSTRAINT c FOREIGN KEY (foreign_id) REFERENCES ref (id)", "table"),
			arguments("CREATE INDEX types_name ON types (name)", "types"),  // different from otel
			arguments("DROP INDEX types_name",  "types_name"),  // different from otel
			arguments("CREATE VIEW tmp AS SELECT type FROM table WHERE id = ?",  "tmp"),  // different from otel
			arguments("CREATE PROCEDURE p AS SELECT * FROM table GO", null)
			// @formatter:on
		);
	}

}
