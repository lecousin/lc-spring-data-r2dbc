package net.lecousin.reactive.data.relational.test.manytomanymodel;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import reactor.core.publisher.Flux;

@Table
public class Entity1 {

	@Id
	@GeneratedValue
	private Long id;

	@Column
	private String value;
	
	@ForeignTable(joinKey = "entity1")
	private List<JoinEntity> links;

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

	public List<JoinEntity> getLinks() {
		return links;
	}

	public void setLinks(List<JoinEntity> links) {
		this.links = links;
	}
	
	public Flux<JoinEntity> lazyGetLinks() {
		return null;
	}
	
}
