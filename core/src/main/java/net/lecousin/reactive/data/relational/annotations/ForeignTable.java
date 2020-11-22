package net.lecousin.reactive.data.relational.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.annotation.Transient;

/**
 * Indicates a foreign key exists on the linked entity.
 * It is transient, meaning there is no corresponding column for this attribute.
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
