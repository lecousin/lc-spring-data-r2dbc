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
package net.lecousin.reactive.data.relational.query.criteria;

import net.lecousin.reactive.data.relational.query.criteria.Criteria.And;
import net.lecousin.reactive.data.relational.query.criteria.Criteria.Or;

/**
 * Interface using the visitor pattern to go through criteria tree.
 * 
 * @author Guillaume Le Cousin
 *
 * @param <T> type of value returned by the visitor
 */
public interface CriteriaVisitor<T> {

	T visit(Criteria.And and);
	
	T visit(Criteria.Or or);
	
	T visit(Criteria.PropertyOperation op);
	
	public abstract static class SearchVisitor implements CriteriaVisitor<Boolean> {
		@Override
		public Boolean visit(And and) {
			return Boolean.valueOf(and.getLeft().accept(this).booleanValue() || and.getRight().accept(this).booleanValue());
		}
		
		@Override
		public Boolean visit(Or or) {
			return Boolean.valueOf(or.getLeft().accept(this).booleanValue() || or.getRight().accept(this).booleanValue());
		}
	}
	
}
