package net.lecousin.reactive.data.relational.test;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;

@Table
public class EntityWithNonNullProperty {

	@Id @GeneratedValue
	private Long id;
	
	@ColumnDefinition(nullable = false)
	private Boolean nonNullable;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Boolean getNonNullable() {
		return nonNullable;
	}

	public void setNonNullable(Boolean nonNullable) {
		this.nonNullable = nonNullable;
	}
	
}
