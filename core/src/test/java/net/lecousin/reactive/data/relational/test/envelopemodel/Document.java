package net.lecousin.reactive.data.relational.test.envelopemodel;

import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.ForeignKey.OnForeignDeleted;
import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;

@Table("edm_doc")
public class Document {

	@Id @GeneratedValue
	private Long id;
	
	@ForeignKey(optional = false, cascadeDelete = false, onForeignDeleted = OnForeignDeleted.DELETE)
	private Envelope envelope;
	
	@Column
	private String documentType;
	
	@ForeignTable(joinKey = "document", optional = false)
	private Set<Page> pages;
	
	@ForeignTable(joinKey = "document", optional = true)
	private Set<Note> notes;

	public Envelope getEnvelope() {
		return envelope;
	}

	public void setEnvelope(Envelope envelope) {
		this.envelope = envelope;
	}

	public String getDocumentType() {
		return documentType;
	}

	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}

	public Set<Page> getPages() {
		return pages;
	}

	public void setPages(Set<Page> pages) {
		this.pages = pages;
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
