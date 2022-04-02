package net.lecousin.reactive.data.relational.test.onetomanymodel;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import reactor.core.publisher.Flux;

@Table
public class RootEntityWithConstructor {

	@Id
	@GeneratedValue
	private Long id;
	
	@Column
	private String value;
	
	@ForeignTable(joinKey = "parent")
	private List<SubEntityWithConstructor> list;
	
	public RootEntityWithConstructor(Long id, String value, List<SubEntityWithConstructor> list) {
		this.id = id;
		this.value = value;
		this.list = list;
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

	public List<SubEntityWithConstructor> getList() {
		return list;
	}

	public void setList(List<SubEntityWithConstructor> list) {
		this.list = list;
	}
	
	public Flux<SubEntityWithConstructor> lazyGetList() {
		return null;
	}

}
