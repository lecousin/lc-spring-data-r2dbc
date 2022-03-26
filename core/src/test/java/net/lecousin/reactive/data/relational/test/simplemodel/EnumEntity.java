package net.lecousin.reactive.data.relational.test.simplemodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.GeneratedValue;

@Table
public class EnumEntity {

	public enum Enum1 {
		V1, V2, V3;
	}
	
	@Id
	@GeneratedValue
	private Long id;

	@Column
	private int i;
	
	@Column
	private Enum1 e1;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getI() {
		return i;
	}

	public void setI(int i) {
		this.i = i;
	}

	public Enum1 getE1() {
		return e1;
	}

	public void setE1(Enum1 e1) {
		this.e1 = e1;
	}
	
}
