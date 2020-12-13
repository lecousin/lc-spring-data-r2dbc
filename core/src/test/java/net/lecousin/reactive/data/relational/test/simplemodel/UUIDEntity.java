package net.lecousin.reactive.data.relational.test.simplemodel;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue.Strategy;

@Table
public class UUIDEntity {

	@Id @GeneratedValue(strategy = Strategy.RANDOM_UUID)
	private UUID uuidKey;
	
	@Column
	private UUID uuidNonKey;

	public UUID getUuidKey() {
		return uuidKey;
	}

	public void setUuidKey(UUID uuidKey) {
		this.uuidKey = uuidKey;
	}

	public UUID getUuidNonKey() {
		return uuidNonKey;
	}

	public void setUuidNonKey(UUID uuidNonKey) {
		this.uuidNonKey = uuidNonKey;
	}

}
