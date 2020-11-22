package net.lecousin.reactive.data.relational.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.relational.core.mapping.Column;

/**
 * Indicates that a field is a foreign key to another table.
 * It is automatically configured as a column, and cannot be used on a collection attribute.
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
	 * In other words, an optional foreign key is nullable.
	 */
	boolean optional() default false;
	
	OnForeignDeleted onForeignKeyDeleted() default OnForeignDeleted.DELETE;
	
}
