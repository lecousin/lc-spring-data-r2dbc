package net.lecousin.reactive.data.relational.test.simplemodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.GeneratedValue;

@Table
public class CharacterTypes {

	@Id
	@GeneratedValue
	private Long id;
	
	@Column
	private char c1;
	
	@Column
	private Character c2;
	
	@Column
	private String str;
	
	@Column
	private char[] chars;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public char getC1() {
		return c1;
	}

	public void setC1(char c1) {
		this.c1 = c1;
	}

	public Character getC2() {
		return c2;
	}

	public void setC2(Character c2) {
		this.c2 = c2;
	}

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}

	public char[] getChars() {
		return chars;
	}

	public void setChars(char[] chars) {
		this.chars = chars;
	}
	
}
