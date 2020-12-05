package net.lecousin.reactive.data.relational.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.relational.core.mapping.Column;

/**
 * Allow to specify information about a column to generate the schema.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
@Column
public @interface ColumnDefinition {
	
	final int DEFAULT_PRECISION = 10;
	final int DEFAULT_SCALE = 2;

	/** Defines is the column may contain NULL or not. */
	boolean nullable() default true;
	/** Defines if the value may be updated or not. */
	boolean updatable() default true;
	
	/** Minimum value or length. */
	long min() default 0;
	/** Maximum value or length. */
	long max() default -1;
	
	/** Floating-point precision. */
	int precision() default DEFAULT_PRECISION;
	/** Floating-point scale. */
	int scale() default DEFAULT_SCALE;
	
}
