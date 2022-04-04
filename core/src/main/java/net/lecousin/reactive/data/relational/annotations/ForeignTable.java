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
package net.lecousin.reactive.data.relational.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.annotation.Transient;

/**
 * Indicates a foreign key exists on the linked entity.
 * It is transient, meaning there is no corresponding column for this attribute.
 * 
 * @author Guillaume Le Cousin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
@Transient
public @interface ForeignTable {
	
	/**
	 * Indicates which attribute in the linked entity is the foreign key to use for this link.
	 */
	String joinKey();
	
	/**
	 * Specifies if the link is optional or not.
	 * If not optional, it means it should always exist a linked entity with a foreign key linked to this entity. 
	 */
	boolean optional() default true;

}
