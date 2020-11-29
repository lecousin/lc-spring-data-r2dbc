package net.lecousin.reactive.data.relational.test.model1;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.CompositeId;
import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.ForeignKey.OnForeignDeleted;

@Table
@CompositeId(indexName = "siteId", properties = { "company", "address" })
public class Site {

	@ForeignKey(optional = false, onForeignDeleted = OnForeignDeleted.DELETE)
	private Company company;
	
	@ForeignKey(optional = false, onForeignDeleted = OnForeignDeleted.DELETE, cascadeDelete = true)
	private PostalAddress address;
	
	@Column
	private String name;

	public Company getCompany() {
		return company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}

	public PostalAddress getAddress() {
		return address;
	}

	public void setAddress(PostalAddress address) {
		this.address = address;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
