package net.lecousin.reactive.data.relational.test.onetoonemodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import reactor.core.publisher.Mono;

@Table
public class MySubEntity2 {

	@Id
	@GeneratedValue
	private Long id;
	
	@Column
	private String subValue;
	
	@ForeignTable(joinKey = "subEntity", optional = false)
	private MyEntity2 parent;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSubValue() {
		return subValue;
	}

	public void setSubValue(String subValue) {
		this.subValue = subValue;
	}

	public MyEntity2 getParent() {
		return parent;
	}

	public void setParent(MyEntity2 parent) {
		this.parent = parent;
	}
	
	public boolean entityLoaded() {
		return true;
	}
	
	public Mono<MySubEntity2> loadEntity() {
		return null;
	}
	
	public Mono<String> lazyGetSubValue() {
		return null;
	}
	
}
