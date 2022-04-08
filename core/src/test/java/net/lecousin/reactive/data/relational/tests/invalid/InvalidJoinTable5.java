package net.lecousin.reactive.data.relational.tests.invalid;

import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import net.lecousin.reactive.data.relational.annotations.JoinTable;

@Table
public class InvalidJoinTable5 {

	@Id @GeneratedValue
	private Long id;
	
	@JoinTable
	private Set<InvalidJoinTable6> join;
	
	public void setJoin() {
	}

}
