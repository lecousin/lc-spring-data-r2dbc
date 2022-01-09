package net.lecousin.reactive.data.relational.test.envelopemodel;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;

@Table("edm_envelope")
public class Envelope {

	@Id @GeneratedValue
	private Long id;
	
	@Column
	private LocalDate receiveDate;
	
	@ForeignTable(joinKey = "envelope", optional = false)
	private Set<Document> documents;
	
	@ForeignTable(joinKey = "envelope", optional = true)
	private Set<Note> notes;

	public LocalDate getReceiveDate() {
		return receiveDate;
	}

	public void setReceiveDate(LocalDate receiveDate) {
		this.receiveDate = receiveDate;
	}

	public Set<Document> getDocuments() {
		return documents;
	}

	public void setDocuments(Set<Document> documents) {
		this.documents = documents;
	}

	public Long getId() {
		return id;
	}

	public Set<Note> getNotes() {
		return notes;
	}

	public void setNotes(Set<Note> notes) {
		this.notes = notes;
	}

}
