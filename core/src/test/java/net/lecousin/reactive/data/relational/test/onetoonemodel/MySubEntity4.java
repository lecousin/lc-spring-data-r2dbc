package net.lecousin.reactive.data.relational.test.onetoonemodel;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.CompositeId;
import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.ForeignKey.OnForeignDeleted;
import reactor.core.publisher.Mono;

@Table
@CompositeId(indexName = "sub_entity_4_key", properties = {"value1", "value2"})
public class MySubEntity4 {

	@Column
	private String value1;
	
	@Column
	private String value2;
	
	@ForeignKey(optional = false)
	private MyEntity4 parent;
	
	@ForeignKey(optional = false, cascadeDelete = true, onForeignDeleted = OnForeignDeleted.DELETE)
	private MyEntity1 entity1;

	public String getValue1() {
		return value1;
	}

	public void setValue1(String value1) {
		this.value1 = value1;
	}

	public String getValue2() {
		return value2;
	}

	public void setValue2(String value2) {
		this.value2 = value2;
	}

	public MyEntity4 getParent() {
		return parent;
	}

	public void setParent(MyEntity4 parent) {
		this.parent = parent;
	}

	public MyEntity1 getEntity1() {
		return entity1;
	}

	public void setEntity1(MyEntity1 entity1) {
		this.entity1 = entity1;
	}
	
	public Mono<MyEntity1> lazyGetEntity1() {
		return null;
	}
}
