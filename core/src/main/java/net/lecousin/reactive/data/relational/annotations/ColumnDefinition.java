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

	boolean nullable() default true;
	
	long min() default 0;
	long max() default -1;
	
	int precision() default DEFAULT_PRECISION;
	int scale() default DEFAULT_SCALE;
	
}
