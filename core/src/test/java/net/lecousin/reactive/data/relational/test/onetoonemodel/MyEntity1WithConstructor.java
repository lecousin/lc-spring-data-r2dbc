package net.lecousin.reactive.data.relational.test.onetoonemodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import reactor.core.publisher.Mono;

@Table
public class MyEntity1WithConstructor {

	@Id
	@GeneratedValue
	private Long id;
	
	@Column("value")
	private String value;
	
	@ForeignTable(joinKey = "parent", optional = true)
	private MySubEntity1WithConstructor subEntity;
	
	public MyEntity1WithConstructor(Long id, String value, MySubEntity1WithConstructor subEntity) {
		this.id = id;
		this.value = value;
		this.subEntity = subEntity;
	}

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

	public MySubEntity1WithConstructor getSubEntity() {
		return subEntity;
	}

	public void setSubEntity(MySubEntity1WithConstructor subEntity) {
		this.subEntity = subEntity;
	}
	
	public Mono<MySubEntity1WithConstructor> lazyGetSubEntity() {
		return null;
	}
	
}
