package net.lecousin.reactive.data.relational.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.annotation.Transient;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
@Transient
public @interface JoinTable {

	/**
	 * Specifies the table name to use for the join.
	 * By default the name is generated by concatenating the name of the two tables. 
	 */
	String tableName() default "";
	
	/**
	 * Specifies the name of the column to use in the join table to store the foreign key to this entity.
	 * By default the name of the entity table is used.
	 */
	String columnName() default "";
	
	/**
	 * Specifies if the linked entities must be deleted when the owning entity is deleted.
	 */
	boolean cascadeDelete() default false;
	
	String joinProperty() default "";
	
}
