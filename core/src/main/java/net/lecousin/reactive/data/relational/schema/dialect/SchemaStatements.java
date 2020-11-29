package net.lecousin.reactive.data.relational.schema.dialect;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SchemaStatements {
	
	private static final Log LOGGER = LogFactory.getLog(SchemaStatements.class);

	private List<SchemaStatement> statements = new LinkedList<>();

	public void add(SchemaStatement statement) {
		statements.add(statement);
	}
	
	public boolean hasStatements() {
		return !statements.isEmpty();
	}
	
	private synchronized List<SchemaStatement> peekReadyStatements() {
		List<SchemaStatement> ready = new LinkedList<>();
		for (Iterator<SchemaStatement> it = statements.iterator(); it.hasNext(); ) {
			SchemaStatement s = it.next();
			if (s.hasDependency())
				continue;
			ready.add(s);
			it.remove();
		}
		return ready;
	}
	
	private synchronized void done(SchemaStatement done) {
		for (SchemaStatement s : statements)
			s.removeDependency(done);
	}
	
	public Mono<Void> execute(LcReactiveDataRelationalClient client) {
		return Flux.just("")
			.expand(s -> execute(client, peekReadyStatements()))
			.then();
	}
	
	private Flux<String> execute(LcReactiveDataRelationalClient client, List<SchemaStatement> statements) {
		return Flux.fromIterable(statements)
			.flatMap(s -> client.getSpringClient().sql(log(s.getSql())).fetch().rowsUpdated().doOnError(e -> log(s, e)).thenReturn(s))
			.doOnNext(this::done)
			.map(SchemaStatement::getSql);
	}

	private static String log(String sql) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(sql);
		return sql;
	}
	
	private static void log(SchemaStatement s, Throwable error) {
		LOGGER.error("Error executing " + s.getSql(), error);
	}
}
