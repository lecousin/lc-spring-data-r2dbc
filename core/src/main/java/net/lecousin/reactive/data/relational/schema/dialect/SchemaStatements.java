package net.lecousin.reactive.data.relational.schema.dialect;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SchemaStatements {

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
			.flatMap(s -> client.getSpringClient().sql(s.getSql()).fetch().rowsUpdated().thenReturn(s))
			.doOnNext(s -> done(s))
			.map(s -> s.getSql());
	}
	
}
