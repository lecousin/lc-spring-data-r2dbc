package net.lecousin.reactive.data.relational.test.onetoonemodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;

@Table
public class MySubEntity1WithConstructor {

	@Id
	@GeneratedValue
	private Long id;
	
	@Column
	private String subValue;
	
	@ForeignKey(optional = false)
	private MyEntity1WithConstructor parent;
	
	public MySubEntity1WithConstructor(Long id, String subValue, MyEntity1WithConstructor parent) {
		this.id = id;
		this.subValue = subValue;
		this.parent = parent;
	}

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

	public MyEntity1WithConstructor getParent() {
		return parent;
	}

	public void setParent(MyEntity1WithConstructor parent) {
		this.parent = parent;
	}
	
}
