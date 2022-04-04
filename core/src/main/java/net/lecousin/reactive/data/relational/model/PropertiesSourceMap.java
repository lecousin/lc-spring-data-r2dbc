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
package net.lecousin.reactive.data.relational.model;

import java.util.Map;

import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

/**
 * Implementation of PropertiesSource from a map of values.
 * 
 * @author Guillaume Le Cousin
 *
 */
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
