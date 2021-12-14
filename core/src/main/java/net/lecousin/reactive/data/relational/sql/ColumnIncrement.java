package net.lecousin.reactive.data.relational.sql;

import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.TableLike;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.data.relational.core.sql.render.RenderNamingStrategy;
import org.springframework.util.Assert;

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
		Assert.notNull(context, "RenderContext must not be null");
		RenderNamingStrategy namingStrategy = context.getNamingStrategy();
		Assert.notNull(namingStrategy, "RenderNamingStrategy must not be null");
		TableLike table = column.getTable();
		Assert.notNull(table, "Table in Column must not be null");
		SqlIdentifier columnIdentifier = SqlIdentifier.from(namingStrategy.getReferenceName(table), namingStrategy.getReferenceName(column));
		return columnIdentifier.toSql(context.getIdentifierProcessing()) + " + 1";
	}

}
