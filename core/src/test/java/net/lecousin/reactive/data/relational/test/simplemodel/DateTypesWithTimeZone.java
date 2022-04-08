package net.lecousin.reactive.data.relational.test.simplemodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;

@Table
public class DateTypesWithTimeZone {

	@Id @GeneratedValue
	private Long id;
	
	@Column
	@ColumnDefinition(precision = 3) // to milliseconds 
	private java.time.OffsetTime timeOffsetTime;
	
	@Column
	@ColumnDefinition(precision = 3) // to milliseconds 
	private java.time.ZonedDateTime timeZonedDateTime;
	
	@Column
	private java.time.OffsetTime timeOffsetTimeWithoutPrecision;
	
	@Column
	private java.time.ZonedDateTime timeZonedDateTimeWithoutPrecision;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public java.time.OffsetTime getTimeOffsetTime() {
		return timeOffsetTime;
	}

	public void setTimeOffsetTime(java.time.OffsetTime timeOffsetTime) {
		this.timeOffsetTime = timeOffsetTime;
	}

	public java.time.ZonedDateTime getTimeZonedDateTime() {
		return timeZonedDateTime;
	}

	public void setTimeZonedDateTime(java.time.ZonedDateTime timeZonedDateTime) {
		this.timeZonedDateTime = timeZonedDateTime;
	}

	public java.time.OffsetTime getTimeOffsetTimeWithoutPrecision() {
		return timeOffsetTimeWithoutPrecision;
	}

	public void setTimeOffsetTimeWithoutPrecision(java.time.OffsetTime timeOffsetTimeWithoutPrecision) {
		this.timeOffsetTimeWithoutPrecision = timeOffsetTimeWithoutPrecision;
	}

	public java.time.ZonedDateTime getTimeZonedDateTimeWithoutPrecision() {
		return timeZonedDateTimeWithoutPrecision;
	}

	public void setTimeZonedDateTimeWithoutPrecision(java.time.ZonedDateTime timeZonedDateTimeWithoutPrecision) {
		this.timeZonedDateTimeWithoutPrecision = timeZonedDateTimeWithoutPrecision;
	}

}
