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

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import org.springframework.data.repository.core.RepositoryMetadata;

/**
 * Spring data repository factory.
 * 
 * @author Guillaume Le Cousin
 *
 */
public class LcR2dbcRepositoryFactory extends R2dbcRepositoryFactory {

	public LcR2dbcRepositoryFactory(R2dbcEntityOperations operations) {
		super(operations);
	}
	
	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return LcR2dbcRepositoryImpl.class;
	}
	
}
