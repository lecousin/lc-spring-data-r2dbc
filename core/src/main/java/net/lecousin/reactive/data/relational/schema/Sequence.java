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
package net.lecousin.reactive.data.relational.schema;

import org.springframework.data.relational.core.sql.IdentifierProcessing;

/**
 * A sequence in a database schema.
 * 
 * @author Guillaume Le Cousin
 *
 */
public class Sequence {

	private String name;
	private IdentifierProcessing idProcessing;
	
	public Sequence(String name, IdentifierProcessing idProcessing) {
		this.name = name;
		this.idProcessing = idProcessing;
	}
	
	public String toSql() {
		return idProcessing.quote(name);
	}
	
	public String getReferenceName() {
		return name;
	}

}
