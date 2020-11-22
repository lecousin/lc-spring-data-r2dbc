package net.lecousin.reactive.data.relational.test.manytomanymodel;

import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ForeignKey;

@Table
public class JoinEntity {

	@ForeignKey(optional = false)
	private Entity1 entity1;
	
	@ForeignKey(optional = false)
	private Entity2 entity2;

	public Entity1 getEntity1() {
		return entity1;
	}

	public void setEntity1(Entity1 entity1) {
		this.entity1 = entity1;
	}

	public Entity2 getEntity2() {
		return entity2;
	}

	public void setEntity2(Entity2 entity2) {
		this.entity2 = entity2;
	}

}
