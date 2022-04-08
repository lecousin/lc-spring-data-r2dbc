package net.lecousin.reactive.data.relational.test.onetoonemodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;

@Table
public class MyEntity6 {

	@Id
	@GeneratedValue
	private Long id;
	
	@Column("value")
	private String value;
	
	@ForeignTable(joinKey = "parent", optional = false)
	private MySubEntity6 subEntity;

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

	public MySubEntity6 getSubEntity() {
		return subEntity;
	}

	public void setSubEntity(MySubEntity6 subEntity) {
		this.subEntity = subEntity;
	}
	
}
