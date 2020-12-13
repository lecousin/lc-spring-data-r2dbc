package net.lecousin.reactive.data.relational.test.simplemodel;

import java.time.ZonedDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.GeneratedValue;

@Table
public class VersionedEntity {

	@Id @GeneratedValue
	private Long id;
	
	@Version
	private Long version;
	
	@Column
	private String str;
	
	@Column @CreatedDate
	private Long creation;
	
	@Column @CreatedDate
	private java.time.Instant creationInstant;
	
	@Column @CreatedDate
	private java.time.LocalDate creationLocalDate;
	
	@Column @CreatedDate
	private java.time.LocalTime creationLocalTime;
	
	@Column @CreatedDate
	private java.time.OffsetTime creationOffsetTime;
	
	@Column @CreatedDate
	private java.time.LocalDateTime creationLocalDateTime;
	
	@Column @CreatedDate
	private java.time.ZonedDateTime creationZonedDateTime;
	
	@Column @LastModifiedDate
	private ZonedDateTime modification;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}

	public Long getCreation() {
		return creation;
	}

	public ZonedDateTime getModification() {
		return modification;
	}

	public java.time.Instant getCreationInstant() {
		return creationInstant;
	}

	public java.time.LocalDate getCreationLocalDate() {
		return creationLocalDate;
	}

	public java.time.LocalTime getCreationLocalTime() {
		return creationLocalTime;
	}

	public java.time.OffsetTime getCreationOffsetTime() {
		return creationOffsetTime;
	}

	public java.time.LocalDateTime getCreationLocalDateTime() {
		return creationLocalDateTime;
	}

	public java.time.ZonedDateTime getCreationZonedDateTime() {
		return creationZonedDateTime;
	}

}
