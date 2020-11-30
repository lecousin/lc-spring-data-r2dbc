package net.lecousin.reactive.data.relational.sql;

import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.data.relational.core.sql.render.RenderNamingStrategy;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;

/**
 * Class to compensate that incrementing a value is not supported by Expression.
 */
public class ColumnIncrement {

	private Column column;
	private LcReactiveDataRelationalClient client;
	
	public ColumnIncrement(Column column, LcReactiveDataRelationalClient client) {
		this.column = column;
		this.client = client;
	}
	
	@Override
	public String toString() {
		RenderContext context = client.getDataAccess().getStatementMapper().getRenderContext();
		RenderNamingStrategy namingStrategy = context.getNamingStrategy();
		SqlIdentifier columnIdentifier = SqlIdentifier.from(namingStrategy.getReferenceName(column.getTable()), namingStrategy.getReferenceName(column));
		return columnIdentifier.toSql(context.getIdentifierProcessing()) + " + 1";
	}

}
