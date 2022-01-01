package net.lecousin.reactive.data.relational.query.operation;

import java.util.LinkedList;
import java.util.List;

import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.lang.Nullable;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.model.EntityCache;
import net.lecousin.reactive.data.relational.query.operation.DeleteProcessor.DeleteRequest;
import net.lecousin.reactive.data.relational.query.operation.SaveProcessor.SaveRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class Operation {
	
	LcReactiveDataRelationalClient lcClient;
	EntityCache cache = new EntityCache();
	EntityLoader loader = new EntityLoader();
	PropertyUpdater updater = new PropertyUpdater();
	SaveProcessor save = new SaveProcessor();
	DeleteProcessor delete = new DeleteProcessor();
	DeleteWithoutLoading deleteWithoutLoading = new DeleteWithoutLoading();

	/** List of functions to call in sequence. */
	private List<Runnable> toCall = new LinkedList<>();
	
	public Operation(LcReactiveDataRelationalClient lcClient) {
		this.lcClient = lcClient;
	}
	
	@SuppressWarnings("unchecked")
	public <T> SaveRequest addToSave(T entity, @Nullable RelationalPersistentEntity<T> entityType, @Nullable EntityState state, @Nullable PersistentPropertyAccessor<T> accessor) {
		SaveRequest request = save.addToProcess(this, entity, entityType, state, accessor);
		delete.addToNotProcess(this, entity, (RelationalPersistentEntity<T>) request.entityType, request.state, (PersistentPropertyAccessor<T>) request.accessor);
		return request;
	}
	
	public <T> DeleteRequest addToDelete(T entity, @Nullable RelationalPersistentEntity<T> entityType, @Nullable EntityState state, @Nullable PersistentPropertyAccessor<T> accessor) {
		return delete.addToProcess(this, entity, entityType, state, accessor);
	}

	void toCall(Runnable fct) {
		synchronized (toCall) {
			toCall.add(fct);
		}
	}
	
	public Mono<Void> execute() {
		return doNext().thenReturn(1).expand(value -> {
			Mono<Void> step = doNext();
			if (step == null)
				return Mono.empty();
			return step.thenReturn(1);
		}).then();
	}

	private Mono<Void> doNext() {
		// call functions
		List<Runnable> calls = toCall;
		toCall = new LinkedList<>();
		for (Runnable r : calls)
			r.run();
		
		// process what need to be processed
		try {
			if (save.processRequests(Operation.this))
				return Mono.empty();
			if (delete.processRequests(Operation.this))
				return Mono.empty();
		} catch (Exception e) {
			return Mono.error(e);
		}
		
		// if some entities need to be loaded, or retrieved, do it
		Mono<Void> op = loader.doOperations(Operation.this);
		if (op != null)
			return op;
		
		// finally we can do the operations
		op = updater.doOperations(Operation.this);
		if (op != null)
			return op;

		op = save.doOperations(Operation.this);
		if (op != null)
			return op;
		
		op = delete.doOperations(Operation.this);
		Mono<Void> op2 = deleteWithoutLoading.doOperations(Operation.this);
		if (op == null)
			return op2;
		if (op2 == null)
			return op;
		return Mono.when(op, op2);
	}

	static Mono<Void> executeParallel(List<Mono<Void>> monos) {
		if (monos.isEmpty())
			return null;
		if (monos.size() == 1)
			return monos.get(0);
		if (monos.size() > 4)
			return Flux.fromIterable(monos)
				.parallel()
				.runOn(Schedulers.parallel(), 4)
				.flatMap(s -> s)
				.then()
				;
		return Flux.merge(monos).then();
	}
}
