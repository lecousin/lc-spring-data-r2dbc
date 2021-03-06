package net.lecousin.reactive.data.relational.test.model1;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.ForeignKey.OnForeignDeleted;
import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import net.lecousin.reactive.data.relational.annotations.Index;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Table
@Index(name = "companyName", properties = { "name" }, unique = true)
public class Company {

	@Id @GeneratedValue
	private Long id;
	
	@ColumnDefinition(min = 1, max = 100, nullable = false)
	private String name;
	
	@Version
	private Long version;
	
	@ForeignTable(joinKey = "company")
	private List<Employee> employees;
	
	@ForeignTable(joinKey = "company")
	private List<Site> sites;
	
	@ForeignTable(joinKey = "owner")
	private PointOfContact[] providers;
	
	@ForeignKey(optional = true, cascadeDelete = false, onForeignDeleted = OnForeignDeleted.SET_TO_NULL)
	private Person owner;

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

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
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
	
	public Mono<Company> loadEntity() {
		return null;
	}
	
	public Flux<Employee> lazyGetEmployees() {
		return null;
	}
	
	public Flux<PointOfContact> lazyGetProviders() {
		return null;
	}
	
	public Flux<Site> lazyGetSites() {
		return null;
	}

	public Person getOwner() {
		return owner;
	}

	public void setOwner(Person owner) {
		this.owner = owner;
	}

}
