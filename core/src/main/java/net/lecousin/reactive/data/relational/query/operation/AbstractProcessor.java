package net.lecousin.reactive.data.relational.query.operation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.util.Pair;
import org.springframework.lang.Nullable;

import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.model.ModelAccessException;
import net.lecousin.reactive.data.relational.model.ModelUtils;
import reactor.core.publisher.Mono;

abstract class AbstractProcessor<R extends AbstractProcessor.Request> {

	/** Requests, by table, by instance. */
	private Map<RelationalPersistentEntity<?>, Map<Object, R>> requests = new HashMap<>();
	
	abstract static class Request {
		RelationalPersistentEntity<?> entityType;
		Object instance;
		EntityState state;
		PersistentPropertyAccessor<?> accessor;
		
		boolean processed = false;
		boolean toProcess = true;
		boolean executed = false;
		
		Set<Request> dependencies = new HashSet<>();
		
		Request(RelationalPersistentEntity<?> entityType, Object instance, EntityState state, PersistentPropertyAccessor<?> accessor) {
			this.entityType = entityType;
			this.instance = instance;
			this.state = state;
			this.accessor = accessor;
		}
		
		void dependsOn(Request dependency) {
			dependencies.add(dependency);
		}
	}
	
	public R addToProcess(Operation op, Object instance, @Nullable RelationalPersistentEntity<?> entity, @Nullable EntityState state, @Nullable PersistentPropertyAccessor<?> accessor) {
		return addRequest(op, instance, entity, state, accessor);
	}
	
	public R addToNotProcess(Operation op, Object instance, @Nullable RelationalPersistentEntity<?> entity, @Nullable EntityState state, @Nullable PersistentPropertyAccessor<?> accessor) {
		R request = addRequest(op, instance, entity, state, accessor);
		request.toProcess = false;
		return request;
	}
	
	boolean processRequests(Operation op) {
		boolean somethingProcessed = false;
		for (Map<Object, R> map : new ArrayList<>(requests.values()))
			for (R request : map.values()) {
				somethingProcessed |= process(op, request);
			}
		return somethingProcessed;
	}
	
	private boolean process(Operation op, R request) {
		if (request.processed || !request.toProcess)
			return false;
		request.processed = true;
		
		if (!checkRequest(op, request))
			return false;
		
		// process foreign keys
		for (RelationalPersistentProperty property : request.entityType) {
			ForeignKey fkAnnotation = property.findAnnotation(ForeignKey.class);
			if (fkAnnotation != null) {
				Pair<Field, ForeignTable> p = ModelUtils.getRequiredForeignTableWithFieldForJoinKey(property.getActualType(), property.getName());
				processForeignKey(op, request, property, fkAnnotation, p.getFirst(), p.getSecond());
			}
		}
		
		// process foreign tables
		for (Pair<Field, ForeignTable> p : ModelUtils.getForeignTables(request.instance.getClass())) {
			boolean isCollection = ModelUtils.isCollection(p.getFirst());
			RelationalPersistentEntity<?> foreignEntity = op.lcClient.getMappingContext().getRequiredPersistentEntity(isCollection ? ModelUtils.getRequiredCollectionType(p.getFirst()) : p.getFirst().getType());
			RelationalPersistentProperty fkProperty = foreignEntity.getRequiredPersistentProperty(p.getSecond().joinKey());
			ForeignKey fk = fkProperty.findAnnotation(ForeignKey.class);
			MutableObject<?> foreignFieldValue;
			try {
				foreignFieldValue = request.state.getForeignTableField(request.instance, p.getFirst().getName());
			} catch (Exception e) {
				throw new ModelAccessException("Unable to get foreign table field", e);
			}
			
			processForeignTableField(op, request, p.getFirst(), p.getSecond(), foreignFieldValue, isCollection, foreignEntity, fkProperty, fk);
		}
		
		return true;
	}
	
	protected abstract R createRequest(Object instance, EntityState state, RelationalPersistentEntity<?> entity, PersistentPropertyAccessor<?> accessor);
	
	protected abstract boolean checkRequest(Operation op, R request);
	
	protected abstract void processForeignKey(Operation op, R request, RelationalPersistentProperty fkProperty, ForeignKey fkAnnotation, Field foreignTableField, ForeignTable foreignTableAnnotation);
	
	@SuppressWarnings("java:S107")
	protected abstract void processForeignTableField(Operation op, R request, Field foreignTableField, ForeignTable foreignTableAnnotation, MutableObject<?> foreignFieldValue, boolean isCollection, RelationalPersistentEntity<?> foreignEntity, RelationalPersistentProperty fkProperty, ForeignKey fkAnnotation);
	
	private R addRequest(Operation op, Object instance, @Nullable RelationalPersistentEntity<?> entity, @Nullable EntityState state, @Nullable PersistentPropertyAccessor<?> accessor) {
		if (entity == null)
			entity = op.lcClient.getMappingContext().getRequiredPersistentEntity(instance.getClass());
		if (accessor == null)
			accessor = entity.getPropertyAccessor(instance);
		instance = op.cache.getInstance(instance, entity, accessor);
		Map<Object, R> map = requests.computeIfAbsent(entity, e -> new HashMap<>());
		R r = map.get(instance);
		if (r == null) {
			if (state == null)
				state = EntityState.get(instance, op.lcClient, entity);
			r = createRequest(instance, state, entity, accessor);
			map.put(instance, r);
		}
		return r;
	}
	
	protected Mono<Void> executeRequests(Operation op) {
		List<Mono<Void>> executions = new LinkedList<>();
		for (Map.Entry<RelationalPersistentEntity<?>, Map<Object, R>> entity : requests.entrySet()) {
			List<R> ready = new LinkedList<>();
			for (R request : entity.getValue().values()) {
				if (executeRequest(request))
					ready.add(request);
			}
			if (!ready.isEmpty()) {
				executions.add(doRequests(op, entity.getKey(), ready).doOnSuccess(v -> {
					for (R r : ready)
						r.executed = true;
				}));
			}
		}
		if (executions.isEmpty())
			return null;
		return Mono.when(executions);
	}
	
	private boolean executeRequest(R request) {
		if (!request.processed || !request.toProcess || request.executed)
			return false;
		for (Iterator<Request> it = request.dependencies.iterator(); it.hasNext(); ) {
			Request dependency = it.next();
			if (!dependency.toProcess || dependency.executed)
				it.remove();
		}
		return request.dependencies.isEmpty();
	}
	
	protected abstract Mono<Void> doRequests(Operation op, RelationalPersistentEntity<?> entityType, List<R> requests);
	
	protected Mono<Void> doOperations(Operation op) {
		return executeRequests(op);
	}
	
}
