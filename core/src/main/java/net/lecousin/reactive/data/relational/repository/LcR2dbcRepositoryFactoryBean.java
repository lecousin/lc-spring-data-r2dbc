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
package net.lecousin.reactive.data.relational.repository;

import java.io.Serializable;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * Spring data repository factory.
 * 
 * @author Guillaume Le Cousin
 *
 * @param <T> type of repository
 * @param <S> type of entity
 * @param <ID> type of primary key
 */
@SuppressWarnings("java:S119") // name of parameter ID
public class LcR2dbcRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable> extends R2dbcRepositoryFactoryBean<T, S, ID> {

	public LcR2dbcRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}
	
	@Override
	protected RepositoryFactorySupport getFactoryInstance(R2dbcEntityOperations operations) {
		return new LcR2dbcRepositoryFactory(operations);
	}

}
