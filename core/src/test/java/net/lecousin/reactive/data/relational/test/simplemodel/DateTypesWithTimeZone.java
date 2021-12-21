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

}
