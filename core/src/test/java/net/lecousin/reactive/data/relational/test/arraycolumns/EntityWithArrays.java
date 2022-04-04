package net.lecousin.reactive.data.relational.test.arraycolumns;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.GeneratedValue;

@Table
public class EntityWithArrays {

	@Id @GeneratedValue
	private Long id;
	
	@Column
	private Boolean[] booleans;
	
	@Column
	private boolean[] primitiveBooleans;
	
	@Column
	private List<Boolean> booleanList;
	
	@Column
	private Short[] shorts;
	
	@Column
	private short[] primitiveShorts;
	
	@Column
	private List<Short> shortList;
	
	@Column
	private Integer[] integers;
	
	@Column
	private int[] primitiveIntegers;
	
	@Column
	private List<Integer> integerList;
	
	@Column
	private Long[] longs;
	
	@Column
	private long[] primitiveLongs;
	
	@Column
	private List<Long> longList;
	
	@Column
	private Float[] floats;
	
	@Column
	private float[] primitiveFloats;
	
	@Column
	private List<Float> floatList;
	
	@Column
	private Double[] doubles;
	
	@Column
	private double[] primitiveDoubles;
	
	@Column
	private List<Double> doubleList;
	
	@Column
	private String[] strings;
	
	@Column
	private List<String> stringList;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer[] getIntegers() {
		return integers;
	}

	public void setIntegers(Integer[] integers) {
		this.integers = integers;
	}

	public int[] getPrimitiveIntegers() {
		return primitiveIntegers;
	}

	public void setPrimitiveIntegers(int[] primitiveIntegers) {
		this.primitiveIntegers = primitiveIntegers;
	}

	public Boolean[] getBooleans() {
		return booleans;
	}

	public void setBooleans(Boolean[] booleans) {
		this.booleans = booleans;
	}

	public boolean[] getPrimitiveBooleans() {
		return primitiveBooleans;
	}

	public void setPrimitiveBooleans(boolean[] primitiveBooleans) {
		this.primitiveBooleans = primitiveBooleans;
	}

	public Short[] getShorts() {
		return shorts;
	}

	public void setShorts(Short[] shorts) {
		this.shorts = shorts;
	}

	public short[] getPrimitiveShorts() {
		return primitiveShorts;
	}

	public void setPrimitiveShorts(short[] primitiveShorts) {
		this.primitiveShorts = primitiveShorts;
	}

	public Long[] getLongs() {
		return longs;
	}

	public void setLongs(Long[] longs) {
		this.longs = longs;
	}

	public long[] getPrimitiveLongs() {
		return primitiveLongs;
	}

	public void setPrimitiveLongs(long[] primitiveLongs) {
		this.primitiveLongs = primitiveLongs;
	}

	public Float[] getFloats() {
		return floats;
	}

	public void setFloats(Float[] floats) {
		this.floats = floats;
	}

	public float[] getPrimitiveFloats() {
		return primitiveFloats;
	}

	public void setPrimitiveFloats(float[] primitiveFloats) {
		this.primitiveFloats = primitiveFloats;
	}

	public Double[] getDoubles() {
		return doubles;
	}

	public void setDoubles(Double[] doubles) {
		this.doubles = doubles;
	}

	public double[] getPrimitiveDoubles() {
		return primitiveDoubles;
	}

	public void setPrimitiveDoubles(double[] primitiveDoubles) {
		this.primitiveDoubles = primitiveDoubles;
	}

	public List<Boolean> getBooleanList() {
		return booleanList;
	}

	public void setBooleanList(List<Boolean> booleanList) {
		this.booleanList = booleanList;
	}

	public List<Short> getShortList() {
		return shortList;
	}

	public void setShortList(List<Short> shortList) {
		this.shortList = shortList;
	}

	public List<Integer> getIntegerList() {
		return integerList;
	}

	public void setIntegerList(List<Integer> integerList) {
		this.integerList = integerList;
	}

	public List<Long> getLongList() {
		return longList;
	}

	public void setLongList(List<Long> longList) {
		this.longList = longList;
	}

	public List<Float> getFloatList() {
		return floatList;
	}

	public void setFloatList(List<Float> floatList) {
		this.floatList = floatList;
	}

	public List<Double> getDoubleList() {
		return doubleList;
	}

	public void setDoubleList(List<Double> doubleList) {
		this.doubleList = doubleList;
	}

	public String[] getStrings() {
		return strings;
	}

	public void setStrings(String[] strings) {
		this.strings = strings;
	}

	public List<String> getStringList() {
		return stringList;
	}

	public void setStringList(List<String> stringList) {
		this.stringList = stringList;
	}
	
}
