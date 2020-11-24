package net.lecousin.reactive.data.relational.query.operation;

import java.util.LinkedList;
import java.util.List;

import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.lang.Nullable;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.query.operation.DeleteProcessor.DeleteRequest;
import net.lecousin.reactive.data.relational.query.operation.SaveProcessor.SaveRequest;
import reactor.core.publisher.Mono;

public class Operation {
	
	LcReactiveDataRelationalClient lcClient;
	OperationEntityCache cache = new OperationEntityCache();
	SaveProcessor save = new SaveProcessor();
	DeleteProcessor delete = new DeleteProcessor();
	EntityLoader loader = new EntityLoader();
	PropertyUpdater updater = new PropertyUpdater();

	/** List of functions to call in sequence. */
	private List<Runnable> toCall = new LinkedList<>();
	
	public Operation(LcReactiveDataRelationalClient lcClient) {
		this.lcClient = lcClient;
	}
	
	public SaveRequest addToSave(Object entity, @Nullable RelationalPersistentEntity<?> entityType, @Nullable EntityState state, @Nullable PersistentPropertyAccessor<?> accessor) {
		SaveRequest request = save.addToProcess(this, entity, entityType, state, accessor);
		delete.addToNotProcess(this, entity, request.entityType, request.state, request.accessor);
		return request;
	}
	
	public DeleteRequest addToDelete(Object entity, @Nullable RelationalPersistentEntity<?> entityType, @Nullable EntityState state, @Nullable PersistentPropertyAccessor<?> accessor) {
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
		
		return delete.doOperations(Operation.this);
	}
	
}
