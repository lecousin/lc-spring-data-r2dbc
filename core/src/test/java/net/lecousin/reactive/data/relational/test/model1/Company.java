package net.lecousin.reactive.data.relational.test.model1;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import reactor.core.publisher.Flux;

@Table
public class Company {

	@Id @GeneratedValue
	private Long id;
	
	@ColumnDefinition(min = 1, max = 100, nullable = false)
	private String name;
	
	@ForeignTable(joinKey = "company")
	private List<Employee> employees;
	
	@ForeignTable(joinKey = "company")
	private List<Site> sites;
	
	@ForeignTable(joinKey = "owner")
	private PointOfContact[] providers;

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

	public List<Employee> getEmployees() {
		return employees;
	}

	public void setEmployees(List<Employee> employees) {
		this.employees = employees;
	}

	public List<Site> getSites() {
		return sites;
	}

	public void setSites(List<Site> sites) {
		this.sites = sites;
	}

	public PointOfContact[] getProviders() {
		return providers;
	}

	public void setProviders(PointOfContact[] providers) {
		this.providers = providers;
	}
	
	public Flux<Employee> lazyGetEmployees() {
		return null;
	}

}
