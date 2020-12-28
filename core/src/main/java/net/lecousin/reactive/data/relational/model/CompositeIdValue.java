package net.lecousin.reactive.data.relational.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CompositeIdValue {

	private Map<String, Object> properties = new HashMap<>();
	
	public void add(String name, Object value) {
		properties.put(name, value);
	}
	
	/** Return true if all id properties are null.
	 * 
	 * @return true if all id properties are null
	 */
	public boolean isNull() {
		for (Object value : properties.values())
			if (value != null)
				return false;
		return true;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CompositeIdValue))
			return false;
		CompositeIdValue id = (CompositeIdValue)obj;
		if (id.properties.size() != properties.size())
			return false;
		for (Map.Entry<String, Object> property : properties.entrySet())
			if (!id.properties.containsKey(property.getKey()) || !Objects.equals(property.getValue(), id.properties.get(property.getKey())))
				return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		int hash = 0;
		for (Object property : properties.values())
			if (property != null)
				hash += property.hashCode();
		return hash;
	}

}
