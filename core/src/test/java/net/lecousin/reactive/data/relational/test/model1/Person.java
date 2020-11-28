package net.lecousin.reactive.data.relational.test.model1;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import reactor.core.publisher.Mono;

@Table
public class Person {

	@Id @GeneratedValue
	private Long id;

	@Column
	private String firstName;
	
	@Column
	private String lastName;
	
	@ForeignTable(joinKey = "person", optional = true)
	private Employee job;

	@ForeignKey(optional = true, cascadeDelete = true)
	private PostalAddress address;
	
	@ForeignTable(joinKey = "owner", optional = true)
	private Company owningCompany;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Employee getJob() {
		return job;
	}

	public void setJob(Employee job) {
		this.job = job;
	}

	public PostalAddress getAddress() {
		return address;
	}

	public void setAddress(PostalAddress address) {
		this.address = address;
	}
	
	public boolean entityLoaded() {
		return false;
	}
	
	public Mono<Employee> lazyGetJob() {
		return null;
	}

	public Company getOwningCompany() {
		return owningCompany;
	}

	public void setOwningCompany(Company owningCompany) {
		this.owningCompany = owningCompany;
	}

}
