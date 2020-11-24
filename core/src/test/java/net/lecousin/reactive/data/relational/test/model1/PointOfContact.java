package net.lecousin.reactive.data.relational.test.model1;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.ForeignKey.OnForeignDeleted;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;

@Table
public class PointOfContact {

	@Id @GeneratedValue
	private Long id;
	
	@ForeignKey(optional = false, onForeignDeleted = OnForeignDeleted.DELETE)
	private Company owner;
	
	@ForeignKey(optional = false, onForeignDeleted = OnForeignDeleted.DELETE)
	private Company provider;
	
	@ForeignKey(optional = false, onForeignDeleted = OnForeignDeleted.SET_TO_NULL)
	private Person person;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Company getOwner() {
		return owner;
	}

	public void setOwner(Company owner) {
		this.owner = owner;
	}

	public Company getProvider() {
		return provider;
	}

	public void setProvider(Company provider) {
		this.provider = provider;
	}

	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

}
