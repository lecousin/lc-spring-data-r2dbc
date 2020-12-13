package net.lecousin.reactive.data.relational.test.simplemodel;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.GeneratedValue;

@Table("NUMTYPES")
public class NumericTypes {

	@Id @GeneratedValue
	private Short id;
	
	@Column
	private byte byte1;
	
	@Column
	private Byte byte2;
	
	@Column
	private short short1;
	
	@Column
	private Short short2;
	
	@Column
	private int int_1;
	
	@Column
	private Integer int_2;
	
	@Column
	private long long1;
	
	@Column
	private Long long2;
	
	@Column
	private float float1;
	
	@Column
	private Float float2;
	
	@Column
	private double double1;
	
	@Column
	private Double double2;
	
	@Column
	private BigDecimal bigDec;

	public Short getId() {
		return id;
	}

	public void setId(Short id) {
		this.id = id;
	}

	public byte getByte1() {
		return byte1;
	}

	public void setByte1(byte byte1) {
		this.byte1 = byte1;
	}

	public Byte getByte2() {
		return byte2;
	}

	public void setByte2(Byte byte2) {
		this.byte2 = byte2;
	}

	public short getShort1() {
		return short1;
	}

	public void setShort1(short short1) {
		this.short1 = short1;
	}

	public Short getShort2() {
		return short2;
	}

	public void setShort2(Short short2) {
		this.short2 = short2;
	}

	public int getInt_1() {
		return int_1;
	}

	public void setInt_1(int int_1) {
		this.int_1 = int_1;
	}

	public Integer getInt_2() {
		return int_2;
	}

	public void setInt_2(Integer int_2) {
		this.int_2 = int_2;
	}

	public long getLong1() {
		return long1;
	}

	public void setLong1(long long1) {
		this.long1 = long1;
	}

	public Long getLong2() {
		return long2;
	}

	public void setLong2(Long long2) {
		this.long2 = long2;
	}

	public float getFloat1() {
		return float1;
	}

	public void setFloat1(float float1) {
		this.float1 = float1;
	}

	public Float getFloat2() {
		return float2;
	}

	public void setFloat2(Float float2) {
		this.float2 = float2;
	}

	public double getDouble1() {
		return double1;
	}

	public void setDouble1(double double1) {
		this.double1 = double1;
	}

	public Double getDouble2() {
		return double2;
	}

	public void setDouble2(Double double2) {
		this.double2 = double2;
	}

	public BigDecimal getBigDec() {
		return bigDec;
	}

	public void setBigDec(BigDecimal bigDec) {
		this.bigDec = bigDec;
	}

}
