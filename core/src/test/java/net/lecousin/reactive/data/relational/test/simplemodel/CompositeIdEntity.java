package net.lecousin.reactive.data.relational.test.simplemodel;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.annotations.CompositeId;

@Table
@CompositeId(indexName = "composite_id_index", properties = {"id1", "id2"})
public class CompositeIdEntity {

	@Column
	private Long id1;
	
	@Column
	private String id2;
	
	@Column
	@ColumnDefinition(max = 20)
	private String str;

	public Long getId1() {
		return id1;
	}

	public void setId1(Long id1) {
		this.id1 = id1;
	}

	public String getId2() {
		return id2;
	}

	public void setId2(String id2) {
		this.id2 = id2;
	}

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}

}
