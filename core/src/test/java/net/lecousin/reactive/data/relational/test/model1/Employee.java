package net.lecousin.reactive.data.relational.test.model1;

import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.ForeignKey.OnForeignDeleted;

@Table
public class Employee {

	@ForeignKey(optional = false, onForeignDeleted = OnForeignDeleted.DELETE)
	private Company company;
	
	@ForeignKey(optional = false, onForeignDeleted = OnForeignDeleted.DELETE)
	private Person person;

	public Company getCompany() {
		return company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}

	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}
	
	public boolean entityLoaded() {
		return false;
	}

}
