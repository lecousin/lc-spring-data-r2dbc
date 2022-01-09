package net.lecousin.reactive.data.relational.test.envelopemodel;

import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.annotations.ForeignKey.OnForeignDeleted;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;

@Table("edm_page")
public class Page {

	@Id @GeneratedValue
	private Long id;
	
	@ForeignKey(optional = false, cascadeDelete = false, onForeignDeleted = OnForeignDeleted.DELETE)
	private Document document;
	
	@Column
	private int pageIndex;
	
	@ForeignTable(joinKey = "page", optional = true)
	private Set<Note> notes;

	public Document getDocument() {
		return document;
	}

	public void setDocument(Document document) {
		this.document = document;
	}

	public int getPageIndex() {
		return pageIndex;
	}

	public void setPageIndex(int pageIndex) {
		this.pageIndex = pageIndex;
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
