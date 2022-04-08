package net.lecousin.reactive.data.relational.tests.invalid;

import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.JoinTable;

@Table
public class InvalidJoinTable1 {

	@JoinTable
	private InvalidJoinTable2 join;

}
