package net.lecousin.reactive.data.relational.test.simplemodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;

@Table
public class CharacterTypes {

	@Id
	@GeneratedValue
	private Integer id;
	
	@Column
	private char c1;
	
	@Column
	private Character c2;
	
	@Column
	private String str;
	
	@Column
	private char[] chars;
	
	@ColumnDefinition(min = 5, max = 5, nullable = false)
	private String fixedLengthString;

	@ColumnDefinition(max = 5000, nullable = true)
	private String longString;
	
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
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

	public String getFixedLengthString() {
		return fixedLengthString;
	}

	public void setFixedLengthString(String fixedLengthString) {
		this.fixedLengthString = fixedLengthString;
	}

	public String getLongString() {
		return longString;
	}

	public void setLongString(String longString) {
		this.longString = longString;
	}
	
}
