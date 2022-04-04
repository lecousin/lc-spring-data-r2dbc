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

import org.springframework.data.relational.core.mapping.Column;

/**
 * Indicates that a field is a foreign key to another table.
 * It is automatically configured as a column, and cannot be used on a collection attribute.
 * 
 * @author Guillaume Le Cousin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
@Column
public @interface ForeignKey {

	enum OnForeignDeleted {
		SET_TO_NULL,
		DELETE;
	}
	
	/**
	 * Specifies if the link is optional or not.
	 * In other words, an optional foreign key is nullable.<br/>
	 * This is almost the same as onForeignDeleted, but we may want an optional link to be deleted on forein deleted,
	 * for example in a tree, the root is nullable, but if a parent is deleted we want the children to be deleted.
	 */
	boolean optional() default false;
	
	OnForeignDeleted onForeignDeleted() default OnForeignDeleted.DELETE;
	
	/**
	 * Specifies if the foreign entity must be deleted when the owning entity is deleted or the foreign key is set to null.
	 */
	boolean cascadeDelete() default false;
	
}
