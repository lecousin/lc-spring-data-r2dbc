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
 * Allow to specify information about a column to generate the schema.
 * 
 * @author Guillaume Le Cousin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
@Column
public @interface ColumnDefinition {
	
	/** Defines is the column may contain NULL or not. */
	boolean nullable() default true;
	/** Defines if the value may be updated or not. */
	boolean updatable() default true;
	
	/** Minimum value or length. */
	long min() default 0;
	/** Maximum value or length. */
	long max() default -1;
	
	/** Floating-point precision. */
	int precision() default -1;
	/** Floating-point scale. */
	int scale() default -1;
	
}
