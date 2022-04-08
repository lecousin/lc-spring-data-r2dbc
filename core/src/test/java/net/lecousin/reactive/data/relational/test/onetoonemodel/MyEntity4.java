package net.lecousin.reactive.data.relational.test.onetoonemodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import reactor.core.publisher.Mono;

@Table
public class MyEntity4 {

	@Id
	@GeneratedValue
	private Long id;
	
	@Column("value")
	private String value;
	
	@ForeignTable(joinKey = "parent", optional = false)
	private MySubEntity4 subEntity;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public MySubEntity4 getSubEntity() {
		return subEntity;
	}

	public void setSubEntity(MySubEntity4 subEntity) {
		this.subEntity = subEntity;
	}
	
	public Mono<MySubEntity4> lazyGetSubEntity() {
		return null;
	}
	
}
