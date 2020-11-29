package net.lecousin.reactive.data.relational.query.operation;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;

import reactor.core.publisher.Mono;

@SuppressWarnings("rawtypes")
abstract class AbstractProcessor<R extends AbstractProcessor.Request> {

	abstract static class Request {
		RelationalPersistentEntity<?> entityType;
		
		boolean executed = false;
		
		Set<Request> dependencies = new HashSet<>();
		
		<T> Request(RelationalPersistentEntity<T> entityType) {
			this.entityType = entityType;
		}
		
		void dependsOn(Request dependency) {
			if (dependency.dependencies.contains(this))
				throw new IllegalStateException("Cyclic dependency between requests");
			dependencies.add(dependency);
		}
		
		protected boolean canExecute() {
			return !executed;
		}
		
		protected boolean isDone() {
			return executed;
		}
	}
	
	protected abstract Mono<Void> executeRequests(Operation op);
	
	protected boolean canExecuteRequest(R request) {
		if (!request.canExecute())
			return false;
		for (Iterator<Request> it = request.dependencies.iterator(); it.hasNext(); ) {
			Request dependency = it.next();
			if (dependency.isDone())
				it.remove();
		}
		return request.dependencies.isEmpty();
	}
	
	protected Mono<Void> doOperations(Operation op) {
		return executeRequests(op);
	}
	
}
