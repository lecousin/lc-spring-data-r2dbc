package net.lecousin.reactive.data.relational.test.simplemodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.GeneratedValue;

@Table
public class DateTypes {

	@Id @GeneratedValue
	private Long id;
	
	@Column
	private java.time.Instant timeInstant;
	
	@Column
	private java.time.LocalDate timeLocalDate;
	
	@Column
	private java.time.LocalTime timeLocalTime;
	
	@Column
	private java.time.OffsetTime timeOffsetTime;
	
	@Column
	private java.time.LocalDateTime timeLocalDateTime;
	
	@Column
	private java.time.ZonedDateTime timeZonedDateTime;

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

	public java.time.OffsetTime getTimeOffsetTime() {
		return timeOffsetTime;
	}

	public void setTimeOffsetTime(java.time.OffsetTime timeOffsetTime) {
		this.timeOffsetTime = timeOffsetTime;
	}

	public java.time.LocalDateTime getTimeLocalDateTime() {
		return timeLocalDateTime;
	}

	public void setTimeLocalDateTime(java.time.LocalDateTime timeLocalDateTime) {
		this.timeLocalDateTime = timeLocalDateTime;
	}

	public java.time.ZonedDateTime getTimeZonedDateTime() {
		return timeZonedDateTime;
	}

	public void setTimeZonedDateTime(java.time.ZonedDateTime timeZonedDateTime) {
		this.timeZonedDateTime = timeZonedDateTime;
	}

}
