package net.lecousin.reactive.data.relational.test.envelopemodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.ForeignKey.OnForeignDeleted;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;

@Table("edm_note")
public class Note {

	@Id @GeneratedValue
	private Long id;
	
	@ForeignKey(optional = true, cascadeDelete = false, onForeignDeleted = OnForeignDeleted.DELETE)
	private Envelope envelope;
	
	@ForeignKey(optional = true, cascadeDelete = false, onForeignDeleted = OnForeignDeleted.DELETE)
	private Document document;
	
	@ForeignKey(optional = true, cascadeDelete = false, onForeignDeleted = OnForeignDeleted.DELETE)
	private Page page;

	@Column
	private String text;

	public Envelope getEnvelope() {
		return envelope;
	}

	public void setEnvelope(Envelope envelope) {
		this.envelope = envelope;
	}

	public Document getDocument() {
		return document;
	}

	public void setDocument(Document document) {
		this.document = document;
	}

	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Long getId() {
		return id;
	}
	
}
