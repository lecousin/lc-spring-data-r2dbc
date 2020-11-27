package net.lecousin.reactive.data.relational.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

import io.r2dbc.spi.Row;

public class PropertiesSourceMap implements PropertiesSource {

	private Map<String, Object> map;
	private Map<String, String> aliases;
	private RelationalPersistentEntity<?> entity;
	
	public PropertiesSourceMap(Map<String, Object> map, Map<String, String> aliases, RelationalPersistentEntity<?> entity) {
		this.map = map;
		this.aliases = aliases;
		this.entity = entity;
	}
	
	@Override
	public Object getSource() {
		return map;
	}
	
	@Override
	public boolean isPropertyPresent(RelationalPersistentProperty property) {
		String alias = aliases.get(property.getName());
		return alias != null && map.containsKey(alias);
	}
	
	@Override
	public Object getPropertyValue(RelationalPersistentProperty property) {
		return map.get(aliases.get(property.getName()));
	}
	
	@Override
	public Row asRow() {
		RowWrapper r = new RowWrapper();
		for (RelationalPersistentProperty property : entity) {
			String fieldAlias = aliases.get(property.getName());
			r.names.add(property.getColumnName().toString());
			r.data.add(map.get(fieldAlias));
		}
		return r;
	}

	private static class RowWrapper implements Row {
		private List<Object> data = new LinkedList<>();
		private List<String> names = new LinkedList<>();
		
		@SuppressWarnings("unchecked")
		@Override
		public <T> T get(int index, Class<T> type) {
			if (index < 0 || index >= data.size())
				return null;
			return (T) data.get(index);
		}
		
		@Override
		public <T> T get(String name, Class<T> type) {
			return get(names.indexOf(name), type);
		}
	}
}
