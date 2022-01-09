package net.lecousin.reactive.data.relational.test.envelopemodel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepositoryFactoryBean;
import net.lecousin.reactive.data.relational.test.AbstractLcReactiveDataRelationalTest;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class)
public class AbstractTestEnvelopeModel extends AbstractLcReactiveDataRelationalTest {

	@Autowired
	private EnvelopeRepository envRepo;
	
	@Override
	protected Collection<Class<?>> usedEntities() {
		return Arrays.asList(Envelope.class, Document.class, Page.class, Note.class);
	}
	
	private static Envelope createEnvelope(LocalDate receiveDate, String... notes) {
		Envelope env = new Envelope();
		env.setReceiveDate(receiveDate);
		env.setDocuments(new HashSet<>());
		env.setNotes(new HashSet<>());
		for (String note : notes) {
			Note n = new Note();
			n.setEnvelope(env);
			n.setText(note);
			env.getNotes().add(n);
		}
		return env;
	}
	
	private static Document createDocument(Envelope env, String type, String... notes) {
		Document doc = new Document();
		doc.setDocumentType(type);
		doc.setEnvelope(env);
		doc.setNotes(new HashSet<>());
		doc.setPages(new HashSet<>());
		env.getDocuments().add(doc);
		for (String note : notes) {
			Note n = new Note();
			n.setDocument(doc);
			n.setText(note);
			doc.getNotes().add(n);
		}
		return doc;
	}
	
	private static void createPage(Document doc, int index, String... notes) {
		Page page = new Page();
		page.setPageIndex(index);
		page.setDocument(doc);
		page.setNotes(new HashSet<>());
		doc.getPages().add(page);
		for (String note : notes) {
			Note n = new Note();
			n.setText(note);
			n.setPage(page);
			page.getNotes().add(n);
		}
	}
	
	private static Envelope createEnvelope(LocalDate receiveDate, String[] notes, List<Tuple3<String, String[], List<String[]>>> docsAndPages) {
		Envelope env = createEnvelope(receiveDate, notes);
		for (Tuple3<String, String[], List<String[]>> docTuple : docsAndPages) {
			Document doc = createDocument(env, docTuple.getT1(), docTuple.getT2());
			int index = 1;
			for (String[] pageNotes : docTuple.getT3()) {
				createPage(doc, index++, pageNotes);
			}
		}
		return env;
	}
	
	private static final Comparator<Page> pageSorter = (p1, p2) -> p1.getPageIndex() - p2.getPageIndex();
	
	@Test
	public void test1() {
		Envelope e1 = createEnvelope(LocalDate.of(2014, 3, 25), new String[] { "hello", "world" }, Arrays.asList(
			Tuples.of("DOC1", new String[] { "note doc 1" }, Arrays.asList(
				new String[] { "page 1.1", "page 1.2", "page 1.3" },
				new String[] { "page 2.1" },
				new String[] { "page 3.1", "page 3.2" }
			))
		));
		Envelope e2 = createEnvelope(LocalDate.of(2016, 2, 13), new String[0], Arrays.asList(
			Tuples.of("DOC2.1", new String[] { "note1 doc 2.1" , "note2 doc 2.1"}, Arrays.asList(
				new String[] { "doc 2.1 page 1.1", "doc 2.1 page 1.2"},
				new String[0],
				new String[] { "doc 2.1 page 3.1" }
			)),
			Tuples.of("DOC2.2", new String[] { "note1 doc 2.2" , "note2 doc 2.2"}, Arrays.asList(
				new String[] { "doc 2.2 page 1.1"},
				new String[] { "doc 2.2 page 3.1", "doc 2.2 page 3.2" }
			))
		));
		Envelope e3 = createEnvelope(LocalDate.of(2020, 12, 3), new String[] { "env 3 note 1", "env 3 note 2" }, Arrays.asList(
			Tuples.of("DOC3.1", new String[] { "note 1 env 3 doc 1", "note 2 env 3 doc 1" }, Arrays.asList(
				new String[] { "env 3 doc 1 page 1.1", "env 3 doc 1 page 1.2" },
				new String[] { "env 3 doc 1 page 2.1" }
			)),
			Tuples.of("DOC3.2", new String[] { "note1 doc 3.2" , "note2 doc 3.2"}, Arrays.asList(
				new String[] { "env 3 doc 2 page 1.1" },
				new String[] { "env 3 doc 2 page 2.1" },
				new String[] { "env 3 doc 2 page 3.1", "env 3 doc 2 page 3.2" }
			))
		));
		
		lcClient.saveAll(e1, e2, e3).block();
		
		Assertions.assertEquals(3, SelectQuery.from(Envelope.class, "env").executeCount(lcClient).block());
		Assertions.assertEquals(5, SelectQuery.from(Document.class, "doc").executeCount(lcClient).block());
		Assertions.assertEquals(13, SelectQuery.from(Page.class, "page").executeCount(lcClient).block());
		Assertions.assertEquals(32, SelectQuery.from(Note.class, "note").executeCount(lcClient).block());
		
		List<Envelope> envelopes = SelectQuery.from(Envelope.class, "env")
			.join("env", "documents", "doc")
			.join("doc", "pages", "page")
			.join("env", "notes", "en")
			.join("doc", "notes", "dn")
			.join("page", "notes", "pn")
			.execute(lcClient).collectList().block();
		Assertions.assertEquals(3, envelopes.size());
		e1 = envelopes.stream().filter(e -> e.getReceiveDate().getYear() == 2014).findAny().get();
		e2 = envelopes.stream().filter(e -> e.getReceiveDate().getYear() == 2016).findAny().get();
		e3 = envelopes.stream().filter(e -> e.getReceiveDate().getYear() == 2020).findAny().get();

		// check notes on envelopes
		Assertions.assertEquals(2, e1.getNotes().size());
		Assertions.assertTrue(e1.getNotes().stream().anyMatch(note -> note.getText().equals("hello")));
		Assertions.assertTrue(e1.getNotes().stream().anyMatch(note -> note.getText().equals("world")));
		Assertions.assertEquals(0, e2.getNotes().size());
		Assertions.assertEquals(2, e3.getNotes().size());
		Assertions.assertTrue(e3.getNotes().stream().anyMatch(note -> note.getText().equals("env 3 note 1")));
		Assertions.assertTrue(e3.getNotes().stream().anyMatch(note -> note.getText().equals("env 3 note 2")));
		
		// check documents
		Assertions.assertEquals(1, e1.getDocuments().size());
		Document doc1_1 = e1.getDocuments().iterator().next();
		Assertions.assertEquals("DOC1", doc1_1.getDocumentType());
		Assertions.assertEquals(2, e2.getDocuments().size());
		Document doc2_1 = e2.getDocuments().stream().filter(doc -> "DOC2.1".equals(doc.getDocumentType())).findAny().get();
		Document doc2_2 = e2.getDocuments().stream().filter(doc -> "DOC2.2".equals(doc.getDocumentType())).findAny().get();
		Assertions.assertEquals(2, e3.getDocuments().size());
		Document doc3_1 = e3.getDocuments().stream().filter(doc -> "DOC3.1".equals(doc.getDocumentType())).findAny().get();
		Document doc3_2 = e3.getDocuments().stream().filter(doc -> "DOC3.2".equals(doc.getDocumentType())).findAny().get();
		
		// check pages
		Assertions.assertEquals(3, doc1_1.getPages().size());
		List<Page> pages1_1 = new ArrayList<>(doc1_1.getPages());
		pages1_1.sort(pageSorter);
		Assertions.assertEquals(1, pages1_1.get(0).getPageIndex());
		Assertions.assertEquals(2, pages1_1.get(1).getPageIndex());
		Assertions.assertEquals(3, pages1_1.get(2).getPageIndex());
		
		Assertions.assertEquals(3, doc2_1.getPages().size());
		List<Page> pages2_1 = new ArrayList<>(doc2_1.getPages());
		pages2_1.sort(pageSorter);
		Assertions.assertEquals(1, pages2_1.get(0).getPageIndex());
		Assertions.assertEquals(2, pages2_1.get(1).getPageIndex());
		Assertions.assertEquals(3, pages2_1.get(2).getPageIndex());
		
		Assertions.assertEquals(2, doc2_2.getPages().size());
		List<Page> pages2_2 = new ArrayList<>(doc2_2.getPages());
		pages2_2.sort(pageSorter);
		Assertions.assertEquals(1, pages2_2.get(0).getPageIndex());
		Assertions.assertEquals(2, pages2_2.get(1).getPageIndex());
		
		Assertions.assertEquals(2, doc3_1.getPages().size());
		List<Page> pages3_1 = new ArrayList<>(doc3_1.getPages());
		pages3_1.sort(pageSorter);
		Assertions.assertEquals(1, pages3_1.get(0).getPageIndex());
		Assertions.assertEquals(2, pages3_1.get(1).getPageIndex());
		
		Assertions.assertEquals(3, doc3_2.getPages().size());
		List<Page> pages3_2 = new ArrayList<>(doc3_2.getPages());
		pages3_2.sort(pageSorter);
		Assertions.assertEquals(1, pages3_2.get(0).getPageIndex());
		Assertions.assertEquals(2, pages3_2.get(1).getPageIndex());
		Assertions.assertEquals(3, pages3_2.get(2).getPageIndex());
		
		// check notes on documents
		Assertions.assertEquals(1, doc1_1.getNotes().size());
		Assertions.assertEquals("note doc 1", doc1_1.getNotes().iterator().next().getText());
		
		Assertions.assertEquals(2, doc2_1.getNotes().size());
		Assertions.assertTrue(doc2_1.getNotes().stream().anyMatch(note -> "note1 doc 2.1".equals(note.getText())));
		Assertions.assertTrue(doc2_1.getNotes().stream().anyMatch(note -> "note2 doc 2.1".equals(note.getText())));
		
		Assertions.assertEquals(2, doc2_2.getNotes().size());
		Assertions.assertTrue(doc2_2.getNotes().stream().anyMatch(note -> "note1 doc 2.2".equals(note.getText())));
		Assertions.assertTrue(doc2_2.getNotes().stream().anyMatch(note -> "note2 doc 2.2".equals(note.getText())));
		
		Assertions.assertEquals(2, doc3_1.getNotes().size());
		Assertions.assertTrue(doc3_1.getNotes().stream().anyMatch(note -> "note 1 env 3 doc 1".equals(note.getText())));
		Assertions.assertTrue(doc3_1.getNotes().stream().anyMatch(note -> "note 2 env 3 doc 1".equals(note.getText())));
		
		Assertions.assertEquals(2, doc3_2.getNotes().size());
		Assertions.assertTrue(doc3_2.getNotes().stream().anyMatch(note -> "note1 doc 3.2".equals(note.getText())));
		Assertions.assertTrue(doc3_2.getNotes().stream().anyMatch(note -> "note2 doc 3.2".equals(note.getText())));
		
		// check notes on pages
		
	}

}
