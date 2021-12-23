package net.lecousin.reactive.data.relational.query;

import java.util.LinkedList;
import java.util.List;

import org.springframework.data.relational.core.sql.Delete;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Insert;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.Update;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.data.relational.core.sql.render.SqlRenderer;
import org.springframework.data.util.Pair;
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.r2dbc.core.binding.BindMarker;
import org.springframework.r2dbc.core.binding.BindMarkers;
import org.springframework.r2dbc.core.binding.BindTarget;
import org.springframework.util.Assert;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;

public class SqlQuery<T> {

	private T query;
	private LcReactiveDataRelationalClient client;
	private BindMarkers markers;
	private List<Pair<BindMarker, Object>> bindings = new LinkedList<>();
	
	public SqlQuery(LcReactiveDataRelationalClient client) {
		this.client = client;
		markers = client.getDialect().getBindMarkersFactory().create();
	}
	
	public LcReactiveDataRelationalClient getClient() {
		return client;
	}

	public T getQuery() {
		return query;
	}

	public void setQuery(T query) {
		this.query = query;
	}
	
	public Expression marker(Object value) {
		BindMarker marker = markers.next();
		bindings.add(Pair.of(marker, value));
		return SQL.bindMarker(marker.getPlaceholder());
	}
	
	protected String finalizeQuery(String query) {
		return query;
	}

	public GenericExecuteSpec execute() {
		PreparedOperation<T> operation = new PreparedOperation<T>() {
			@Override
			public T getSource() {
				return query;
			}
			
			@Override
			public void bindTo(BindTarget target) {
				for (Pair<BindMarker, Object> binding : bindings)
					binding.getFirst().bind(target, binding.getSecond());
			}
			
			@Override
			public String toQuery() {
				Assert.notNull(query, "Query must be set");
				RenderContext renderContext = client.getDataAccess().getStatementMapper().getRenderContext();
				SqlRenderer renderer = renderContext != null ? SqlRenderer.create(renderContext) : SqlRenderer.create();
				if (query instanceof Select)
					return finalizeQuery(renderer.render((Select)query));
				if (query instanceof Insert)
					return finalizeQuery(renderer.render((Insert)query));
				if (query instanceof Update)
					return finalizeQuery(renderer.render((Update)query));
				if (query instanceof Delete)
					return finalizeQuery(renderer.render((Delete)query));
				throw new IllegalArgumentException("Unexpected query type: " + query.getClass().getName());
			}
		};
		return client.getSpringClient().sql(operation);
	}

}
