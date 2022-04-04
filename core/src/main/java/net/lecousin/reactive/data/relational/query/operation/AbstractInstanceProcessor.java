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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.lang.Nullable;

import net.lecousin.reactive.data.relational.model.ModelAccessException;
import net.lecousin.reactive.data.relational.model.metadata.EntityInstance;
import net.lecousin.reactive.data.relational.model.metadata.EntityMetadata;
import net.lecousin.reactive.data.relational.model.metadata.EntityStaticMetadata;
import net.lecousin.reactive.data.relational.model.metadata.PropertyMetadata;
import net.lecousin.reactive.data.relational.model.metadata.PropertyStaticMetadata;
import reactor.core.publisher.Mono;

/**
 * Abstract class to process requests on specific entity instances.
 * 
 * @author Guillaume Le Cousin
 *
 * @param <R> type of request
 */
@SuppressWarnings("rawtypes")
abstract class AbstractInstanceProcessor<R extends AbstractInstanceProcessor.Request> extends AbstractProcessor<R> {

	/** Requests, by table, by instance. */
	private Map<EntityMetadata, Map<Object, R>> requests = new HashMap<>();
	
	abstract static class Request extends AbstractProcessor.Request {
		EntityInstance<?> entity;
		
		boolean processed = false;
		boolean toProcess = true;
		
		<T> Request(EntityInstance<T> entity) {
			this.entity = entity;
		}
		
		@Override
		protected boolean canExecute() {
			return processed && super.canExecute();
		}
		
		@Override
		protected boolean isDone() {
			return !toProcess || super.isDone();
		}
	}
	
	public <T> R addToProcess(Operation op, EntityInstance<T> instance) {
		return addRequest(op, instance);
	}
	
	public <T> R addToNotProcess(Operation op, EntityInstance<T> instance) {
		R request = addRequest(op, instance);
		request.toProcess = false;
		return request;
	}
	
	List<R> getPendingRequests(EntityMetadata type, Predicate<R> predicate) {
		List<R> list = new LinkedList<>();
		Map<Object, R> map = requests.get(type);
		if (map == null)
			return list;
		for (R request : map.values()) {
			if (request.toProcess && !request.executed && request.entity.getState().isPersisted() && request.entity.getState().isLoaded() && predicate.test(request))
				list.add(request);
		}
		return list;
	}
	
	boolean processRequests(Operation op) {
		boolean somethingProcessed = false;
		for (Map<Object, R> map : new ArrayList<>(requests.values()))
			for (R request : new ArrayList<>(map.values())) {
				somethingProcessed |= process(op, request);
			}
		return somethingProcessed;
	}
	
	private boolean process(Operation op, R request) {
		if (request.processed || !request.toProcess)
			return false;
		request.processed = true;
		
		if (!doProcess(op, request))
			return false;
		
		processForeignKeys(op, request);
		processForeignTables(op, request);
		
		return true;
	}
	
	private void processForeignKeys(Operation op, R request) {
		for (PropertyMetadata property : request.entity.getMetadata().getForeignKeys()) {
			PropertyStaticMetadata foreignTable = EntityStaticMetadata.get(property.getType()).getForeignTableForJoinKey(property.getName(), request.entity.getType());
			processForeignKey(op, request, property, foreignTable);
		}
	}
	
	private void processForeignTables(Operation op, R request) {
		for (PropertyStaticMetadata foreignTable : EntityStaticMetadata.get(request.entity.getType()).getForeignTables()) {
			EntityMetadata foreignEntityType = op.lcClient.getRequiredEntity(foreignTable.getTypeOrCollectionElementType());
			PropertyMetadata fkProperty = foreignEntityType.getRequiredPersistentProperty(foreignTable.getForeignTableAnnotation().joinKey());
			MutableObject<?> foreignFieldValue;
			try {
				foreignFieldValue = request.entity.getState().getForeignTableField(request.entity.getEntity(), foreignTable);
			} catch (Exception e) {
				throw new ModelAccessException("Unable to get foreign table field", e);
			}
			
			processForeignTableField(op, request, foreignTable, foreignFieldValue, fkProperty);
		}
	}
	
	protected abstract <T> R createRequest(EntityInstance<T> instance);
	
	protected abstract boolean doProcess(Operation op, R request);
	
	protected abstract void processForeignKey(Operation op, R request, PropertyMetadata fkProperty, @Nullable PropertyStaticMetadata foreignTableInfo);
	
	protected abstract void processForeignTableField(Operation op, R request, PropertyStaticMetadata foreignTableInfo, @Nullable MutableObject<?> foreignFieldValue, PropertyMetadata fkProperty);
	
	@SuppressWarnings({ "java:S3824", "unchecked" })
	private <T> R addRequest(Operation op, EntityInstance<T> instance) {
		instance = op.cache.getOrSetInstance(instance);
		Map<Object, R> map = requests.computeIfAbsent(instance.getMetadata(), e -> new HashMap<>());
		R r = map.get(instance.getEntity());
		if (r == null) {
			r = createRequest(instance);
			map.put(instance.getEntity(), r);
		}
		return r;
	}
	
	@Override
	protected Mono<Void> executeRequests(Operation op) {
		List<Mono<Void>> executions = new LinkedList<>();
		for (Map.Entry<EntityMetadata, Map<Object, R>> entity : requests.entrySet()) {
			List<R> ready = new LinkedList<>();
			for (R request : entity.getValue().values()) {
				if (canExecuteRequest(request))
					ready.add(request);
			}
			if (!ready.isEmpty()) {
				Mono<Void> execution = doRequests(op, entity.getKey(), ready);
				if (execution != null)
					executions.add(execution.doOnSuccess(v -> ready.forEach(r -> r.executed = true)));
				else ready.forEach(r -> r.executed = true);
			}
		}
		return Operation.executeParallel(executions);
	}
	
	protected abstract Mono<Void> doRequests(Operation op, EntityMetadata entityType, List<R> requests);
	
}
