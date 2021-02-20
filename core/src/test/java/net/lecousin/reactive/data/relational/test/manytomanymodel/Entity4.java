package net.lecousin.reactive.data.relational.test.manytomanymodel;

import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import net.lecousin.reactive.data.relational.annotations.JoinTable;
import reactor.core.publisher.Flux;

@Table
public class Entity4 {

	@Id
	@GeneratedValue
	private Long id;

	@Column
	private String value;
	
	@JoinTable
	private Set<Entity3> links;

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

	public Set<Entity3> getLinks() {
		return links;
	}

	public void setLinks(Set<Entity3> links) {
		this.links = links;
	}

	public Flux<Entity3> lazyGetLinks() {
		return null;
	}

}
