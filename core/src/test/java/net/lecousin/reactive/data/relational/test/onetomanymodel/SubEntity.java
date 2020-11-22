package net.lecousin.reactive.data.relational.test.onetomanymodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;

@Table
public class SubEntity {

	@Id
	@GeneratedValue
	private Long id;
	
	@Column
	private String subValue;
	
	@ForeignKey(optional = false)
	private RootEntity parent;

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

	public RootEntity getParent() {
		return parent;
	}

	public void setParent(RootEntity parent) {
		this.parent = parent;
	}
	
}
