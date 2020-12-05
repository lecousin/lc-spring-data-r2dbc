package net.lecousin.reactive.data.relational.test.treemodel;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.ForeignKey.OnForeignDeleted;
import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Table
public class Node {

	@Id @GeneratedValue
	private Long id;
	
	@Column
	private String name;
	
	@ForeignKey(optional = true, cascadeDelete = true, onForeignDeleted = OnForeignDeleted.DELETE)
	private Node parent;
	
	@ForeignTable(joinKey = "parent")
	private List<Node> children;
	
	public Node() {
		// default
	}
	
	public Node(Node parent, String name) {
		this.parent = parent;
		this.name = name;
		if (parent != null && parent.getChildren() != null)
			parent.getChildren().add(this);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Node getParent() {
		return parent;
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

	public List<Node> getChildren() {
		return children;
	}

	public void setChildren(List<Node> children) {
		this.children = children;
	}
	
	public Mono<Node> lazyGetParent() {
		return null;
	}
	
	public Flux<Node> lazyGetChildren() {
		return null;
	}

}
