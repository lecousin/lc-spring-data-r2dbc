package net.lecousin.reactive.data.relational.test.model1;

import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.ForeignKey.OnForeignDeleted;

@Table
public class Site {

	@ForeignKey(optional = false, onForeignDeleted = OnForeignDeleted.DELETE)
	private Company company;
	
	@ForeignKey(optional = false, onForeignDeleted = OnForeignDeleted.DELETE, cascadeDelete = true)
	private PostalAddress address;

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

}
