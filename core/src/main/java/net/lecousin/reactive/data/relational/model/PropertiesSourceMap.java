package net.lecousin.reactive.data.relational.model;

import java.util.Map;

import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

public class PropertiesSourceMap implements PropertiesSource {

	private Map<String, Object> map;
	private Map<String, String> aliases;
	
	public PropertiesSourceMap(Map<String, Object> map, Map<String, String> aliases) {
		this.map = map;
		this.aliases = aliases;
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
	
}
