package net.lecousin.reactive.data.relational.test.simplemodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;

@Table
public class DateTypes {

	@Id @GeneratedValue
	private Long id;
	
	@Column
	@ColumnDefinition(precision = 3) // to milliseconds 
	private java.time.Instant timeInstant;
	
	@Column
	private java.time.LocalDate timeLocalDate;
	
	@Column
	@ColumnDefinition(precision = 3) // to milliseconds 
	private java.time.LocalTime timeLocalTime;
	
	@Column
	@ColumnDefinition(precision = 3) // to milliseconds 
	private java.time.LocalDateTime timeLocalDateTime;
	
	@Column
	private java.time.LocalDateTime timeLocalDateTimeWithoutPrecision;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public java.time.Instant getTimeInstant() {
		return timeInstant;
	}

	public void setTimeInstant(java.time.Instant timeInstant) {
		this.timeInstant = timeInstant;
	}

	public java.time.LocalDate getTimeLocalDate() {
		return timeLocalDate;
	}

	public void setTimeLocalDate(java.time.LocalDate timeLocalDate) {
		this.timeLocalDate = timeLocalDate;
	}

	public java.time.LocalTime getTimeLocalTime() {
		return timeLocalTime;
	}

	public void setTimeLocalTime(java.time.LocalTime timeLocalTime) {
		this.timeLocalTime = timeLocalTime;
	}

	public java.time.LocalDateTime getTimeLocalDateTime() {
		return timeLocalDateTime;
	}

	public void setTimeLocalDateTime(java.time.LocalDateTime timeLocalDateTime) {
		this.timeLocalDateTime = timeLocalDateTime;
	}

	public java.time.LocalDateTime getTimeLocalDateTimeWithoutPrecision() {
		return timeLocalDateTimeWithoutPrecision;
	}

	public void setTimeLocalDateTimeWithoutPrecision(java.time.LocalDateTime timeLocalDateTimeWithoutPrecision) {
		this.timeLocalDateTimeWithoutPrecision = timeLocalDateTimeWithoutPrecision;
	}

}
