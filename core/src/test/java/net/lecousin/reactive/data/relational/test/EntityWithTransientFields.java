package net.lecousin.reactive.data.relational.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;

@Table("basic")
public class EntityWithTransientFields {

	@Column
	private String str;
	
	@Transient
	private String textNotSaved;
	
	@Value("${test:hello}")
	private String defaultHello;
	
	@Autowired(required = false)
	private LcReactiveDataRelationalClient client;

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}

	public String getTextNotSaved() {
		return textNotSaved;
	}

	public void setTextNotSaved(String textNotSaved) {
		this.textNotSaved = textNotSaved;
	}

	public String getDefaultHello() {
		return defaultHello;
	}

	public void setDefaultHello(String defaultHello) {
		this.defaultHello = defaultHello;
	}

	public LcReactiveDataRelationalClient getClient() {
		return client;
	}

}
