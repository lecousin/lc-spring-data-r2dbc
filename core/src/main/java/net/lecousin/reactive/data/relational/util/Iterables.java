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
package net.lecousin.reactive.data.relational.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Utilities for iterators and iterables.
 * 
 * @author Guillaume Le Cousin
 *
 */
public class Iterables {

	private Iterables() {
		// no instance
	}
	
	public static <T> Iterable<T> filter(Iterable<T> iterable, Predicate<T> predicate) {
		return () -> filter(iterable.iterator(), predicate);
	}
	
	@SuppressWarnings("java:S1171")
	public static <T> Iterator<T> filter(Iterator<T> iterator, Predicate<T> predicate) {
		return new Iterator<>() {
			private boolean hasNext = true;
			private T next;

			{
				goNext();
			}
			
			private void goNext() {
				while (iterator.hasNext()) {
					T element = iterator.next();
					if (predicate.test(element)) {
						next = element;
						return;
					}
				}
				hasNext = false;
			}
			
			@Override
			public boolean hasNext() {
				return hasNext;
			}
			
			@Override
			public T next() {
				if (!hasNext)
					throw new NoSuchElementException();
				T element = next;
				goNext();
				return element;
			}
		};
	}
	
	public static <T, U> Iterable<U> map(Iterable<T> iterable, Function<T, U> mapper) {
		return () -> map(iterable.iterator(), mapper);
	}
	
	public static <T, U> Iterator<U> map(Iterator<T> iterator, Function<T, U> mapper) {
		return new Iterator<>() {
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}
			
			@Override
			public U next() {
				return mapper.apply(iterator.next());
			}
		};
	}

}
