package net.lecousin.reactive.data.relational.test;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("basic")
public class Entity {

	@Column
	private String str;

}
