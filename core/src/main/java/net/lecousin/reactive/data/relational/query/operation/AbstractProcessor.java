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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import reactor.core.publisher.Mono;

/**
 * Abstract class to process requests.
 * 
 * @author Guillaume Le Cousin
 *
 * @param <R> type of request
 */
@SuppressWarnings("rawtypes")
abstract class AbstractProcessor<R extends AbstractProcessor.Request> {

	abstract static class Request {
		boolean executed = false;
		
		Set<Request> dependencies = new HashSet<>();
		
		Request() {
			// nothing
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
