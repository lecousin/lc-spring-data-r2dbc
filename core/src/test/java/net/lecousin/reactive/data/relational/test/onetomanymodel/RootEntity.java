package net.lecousin.reactive.data.relational.test.onetomanymodel;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import reactor.core.publisher.Flux;

@Table
public class RootEntity {

	@Id
	@GeneratedValue
	private Long id;
	
	@Column
	private String value;
	
	@ForeignTable(joinKey = "parent")
	private List<SubEntity> list;
	
	@ForeignTable(joinKey = "parent")
	private List<SubEntity2> list2;
	
	@ForeignTable(joinKey = "parent")
	private List<SubEntity3> list3;

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

	public List<SubEntity> getList() {
		return list;
	}

	public void setList(List<SubEntity> list) {
		this.list = list;
	}
	
	public Flux<SubEntity> lazyGetList() {
		return null;
	}

	public List<SubEntity2> getList2() {
		return list2;
	}

	public void setList2(List<SubEntity2> list2) {
		this.list2 = list2;
	}
	
	public Flux<SubEntity2> lazyGetList2() {
		return null;
	}

	public List<SubEntity3> getList3() {
		return list3;
	}

	public void setList3(List<SubEntity3> list3) {
		this.list3 = list3;
	}
	
	public Flux<SubEntity3> lazyGetList3() {
		return null;
	}
	
}
