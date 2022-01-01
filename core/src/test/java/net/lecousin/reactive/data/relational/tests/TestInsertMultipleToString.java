package net.lecousin.reactive.data.relational.tests;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.Table;

import net.lecousin.reactive.data.relational.query.InsertMultiple;

class TestInsertMultipleToString {

	@Test
	void test() {
		Table table = Table.create("my_table");
		Column col1 = Column.create("col1", table);
		Column col2 = Column.create("col2", table);
		Column col3 = Column.create("col3", table);
		List<List<Expression>> values = new LinkedList<>();
		values.add(Arrays.asList(SQL.literalOf(true), SQL.literalOf(10), SQL.nullLiteral()));
		values.add(Arrays.asList(SQL.nullLiteral(), SQL.literalOf(false), SQL.literalOf(5)));
		InsertMultiple im = new InsertMultiple(table, Arrays.asList(col1, col2, col3), values);
		Assertions.assertEquals("INSERT INTO my_table (col1,col2,col3) VALUES (TRUE,10,NULL),(NULL,FALSE,5)", im.toString());
		Assertions.assertEquals("INSERT INTO my_table (col1,col2,col3) VALUES (TRUE,10,NULL),(NULL,FALSE,5)", im.render(null));
	}
	
}
