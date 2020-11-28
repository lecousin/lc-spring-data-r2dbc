package net.lecousin.reactive.data.relational.query;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.relational.core.sql.Delete;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Insert;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.Update;
import org.springframework.data.relational.core.sql.render.SqlRenderer;
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
	private Map<BindMarker, Object> bindings = new HashMap<>();
	
	public SqlQuery(LcReactiveDataRelationalClient client) {
		this.client = client;
		markers = client.getDialect().getBindMarkersFactory().create();
	}
	
	public T getQuery() {
		return query;
	}

	public void setQuery(T query) {
		this.query = query;
	}

	public BindMarkers getMarkers() {
		return markers;
	}

	public Map<BindMarker, Object> getBindings() {
		return bindings;
	}
	
	public Expression marker(Object value) {
		BindMarker marker = markers.next();
		bindings.put(marker, value);
		return SQL.bindMarker(marker.getPlaceholder());
	}

	public GenericExecuteSpec execute() {
		PreparedOperation<T> operation = new PreparedOperation<T>() {
			@Override
			public T getSource() {
				return query;
			}
			
			@Override
			public void bindTo(BindTarget target) {
				for (Map.Entry<BindMarker, Object> binding : bindings.entrySet())
					binding.getKey().bind(target, binding.getValue());
			}
			
			@Override
			public String toQuery() {
				Assert.notNull(query, "Query must be set");
				if (query instanceof Select)
					return SqlRenderer.create(client.getDataAccess().getStatementMapper().getRenderContext()).render((Select)query);
				if (query instanceof Insert)
					return SqlRenderer.create(client.getDataAccess().getStatementMapper().getRenderContext()).render((Insert)query);
				if (query instanceof Update)
					return SqlRenderer.create(client.getDataAccess().getStatementMapper().getRenderContext()).render((Update)query);
				if (query instanceof Delete)
					return SqlRenderer.create(client.getDataAccess().getStatementMapper().getRenderContext()).render((Delete)query);
				throw new IllegalArgumentException("Unexpected query type: " + query.getClass().getName());
			}
		};
		return client.getSpringClient().sql(operation);
	}

}
