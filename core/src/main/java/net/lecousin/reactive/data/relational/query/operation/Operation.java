/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.lecousin.reactive.data.relational.query.operation;

import java.util.LinkedList;
import java.util.List;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.model.EntityCache;
import net.lecousin.reactive.data.relational.model.metadata.EntityInstance;
import net.lecousin.reactive.data.relational.query.operation.DeleteProcessor.DeleteRequest;
import net.lecousin.reactive.data.relational.query.operation.SaveProcessor.SaveRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Orchestrate a global save operation, with underlying insert, update and delete necessary to synchronize the database with the entities.
 * 
 * @author Guillaume Le Cousin
 *
 */
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
	public <T> SaveRequest addToSave(EntityInstance<T> entity) {
		SaveRequest request = save.addToProcess(this, entity);
		delete.addToNotProcess(this, entity);
		return request;
	}
	
	public <T> DeleteRequest addToDelete(EntityInstance<T> entity) {
		return delete.addToProcess(this, entity);
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
		if (op != null)
			return op;
		return deleteWithoutLoading.doOperations(Operation.this);
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
