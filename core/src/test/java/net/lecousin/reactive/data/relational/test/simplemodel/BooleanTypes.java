package net.lecousin.reactive.data.relational.test.simplemodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import reactor.core.publisher.Mono;

@Table
public class BooleanTypes {

	@Id
	@GeneratedValue
	private Long id;
	
	@Column
	private Boolean b1;
	
	@Column
	private boolean b2;

	public Long getId() {
		return id;
	}

	public Boolean getB1() {
		return b1;
	}

	public void setB1(Boolean b1) {
		this.b1 = b1;
	}

	public boolean isB2() {
		return b2;
	}

	public void setB2(boolean b2) {
		this.b2 = b2;
	}
	
	public String entityLoaded() {
		return "Not eligible";
	}
	
	public boolean entityLoaded(boolean test) {
		return test;
	}
	
	public Mono<BooleanTypes> loadEntity(@SuppressWarnings("unused") boolean test) {
		return null;
	}
	
	public boolean loadEntity() {
		return false;
	}
}
