/*
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
 * 
 * @author Guillaume Le Cousin
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
