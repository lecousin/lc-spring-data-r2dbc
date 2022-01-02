package net.lecousin.reactive.data.relational.query;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.Named;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.core.sql.render.NamingStrategies;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.data.relational.core.sql.render.RenderNamingStrategy;
import org.springframework.data.relational.core.sql.render.SelectRenderContext;
import org.springframework.util.StringUtils;

public class InsertMultiple {

	private final Table into;
	private final List<Column> columns;
	private final List<InsertRowValues> values;

	public InsertMultiple(Table into, List<Column> columns, List<List<Expression>> values) {
		this.into = into;
		this.columns = new ArrayList<>(columns);
		this.values = new ArrayList<>(values.size());
		for (List<Expression> list : values)
			this.values.add(new InsertRowValues(new ArrayList<>(list)));
	}
	
	public String render(RenderContext renderContext) {
		if (renderContext == null)
			renderContext = new SimpleRenderContext();
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO ");
		sql.append(render(renderContext.getNamingStrategy().getName(into), renderContext));
		sql.append(" (");
		boolean first = true;
		for (Column col : columns) {
			if (first)
				first = false;
			else
				sql.append(',');
			sql.append(render(renderContext.getNamingStrategy().getName(col), renderContext));
		}
		sql.append(") VALUES ");
		first = true;
		for (InsertRowValues row : values) {
			if (first)
				first = false;
			else
				sql.append(',');
			sql.append('(');
			boolean firstValue = true;
			for (Expression value : row.expressions) {
				if (firstValue)
					firstValue = false;
				else
					sql.append(',');
				sql.append(render(value, renderContext));
			}
			sql.append(')');
		}
		return sql.toString();
	}
	
	private static String render(SqlIdentifier identifier, RenderContext renderContext) {
		return identifier.toSql(renderContext.getIdentifierProcessing());
	}
	
	private static String render(Expression expression, RenderContext renderContext) {
		if (expression instanceof Named)
			return render(((Named)expression).getName(), renderContext);
		return expression.toString();
	}
	
	static class SimpleRenderContext implements RenderContext {

		private final RenderNamingStrategy namingStrategy;

		SimpleRenderContext() {
			this.namingStrategy = NamingStrategies.asIs();
		}

		@Override
		public IdentifierProcessing getIdentifierProcessing() {
			return IdentifierProcessing.NONE;
		}

		@Override
		public SelectRenderContext getSelect() {
			return DefaultSelectRenderContext.INSTANCE;
		}

		@Override
		public RenderNamingStrategy getNamingStrategy() {
			return this.namingStrategy;
		}

		enum DefaultSelectRenderContext implements SelectRenderContext {
			INSTANCE;
		}

	}


	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();

		builder.append("INSERT INTO ").append(this.into);

		if (!this.columns.isEmpty()) {
			builder.append(" (").append(StringUtils.collectionToDelimitedString(this.columns.stream().map(col -> col.getName().toString()).collect(Collectors.toList()), ",")).append(")");
		}

		if (!this.values.isEmpty()) {
			builder.append(" VALUES ");
			builder.append(StringUtils.collectionToDelimitedString(this.values, ","));
		}

		return builder.toString();
	}
	
	public static class InsertRowValues {

		private List<Expression> expressions;
		
		InsertRowValues(List<Expression> expressions) {
			this.expressions = expressions;
		}
		
		@Override
		public String toString() {
			return "(" + StringUtils.collectionToDelimitedString(this.expressions, ",") + ")";
		}
		
	}

}
