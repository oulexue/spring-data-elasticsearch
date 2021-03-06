/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.repository.query;

import static org.assertj.core.api.Assertions.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.entities.Person;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

/**
 * @author Christoph Strobl
 * @currentRead Fool's Fate - Robin Hobb
 */
public class ReactiveElasticsearchQueryMethodUnitTests {

	SimpleElasticsearchMappingContext mappingContext;

	@Before
	public void setUp() {
		mappingContext = new SimpleElasticsearchMappingContext();
	}

	@Test // DATAES-519
	public void detectsCollectionFromRepoTypeIfReturnTypeNotAssignable() throws Exception {

		ReactiveElasticsearchQueryMethod queryMethod = queryMethod(NonReactiveRepository.class, "method");
		ElasticsearchEntityMetadata<?> metadata = queryMethod.getEntityInformation();

		assertThat(metadata.getJavaType()).isAssignableFrom(Person.class);
		assertThat(metadata.getIndexName()).isEqualTo("test-index-person");
		assertThat(metadata.getIndexTypeName()).isEqualTo("user");
	}

	@Test(expected = IllegalArgumentException.class) // DATAES-519
	public void rejectsNullMappingContext() throws Exception {

		Method method = PersonRepository.class.getMethod("findByName", String.class);

		new ReactiveElasticsearchQueryMethod(method, new DefaultRepositoryMetadata(PersonRepository.class),
				new SpelAwareProxyProjectionFactory(), null);
	}

	@Test(expected = IllegalStateException.class) // DATAES-519
	public void rejectsMonoPageableResult() throws Exception {
		queryMethod(PersonRepository.class, "findMonoByName", String.class, Pageable.class);
	}

	@Test(expected = InvalidDataAccessApiUsageException.class) // DATAES-519
	public void throwsExceptionOnWrappedPage() throws Exception {
		queryMethod(PersonRepository.class, "findMonoPageByName", String.class, Pageable.class);
	}

	@Test(expected = InvalidDataAccessApiUsageException.class) // DATAES-519
	public void throwsExceptionOnWrappedSlice() throws Exception {
		queryMethod(PersonRepository.class, "findMonoSliceByName", String.class, Pageable.class);
	}

	@Test // DATAES-519
	public void allowsPageableOnFlux() throws Exception {
		queryMethod(PersonRepository.class, "findByName", String.class, Pageable.class);
	}

	@Test // DATAES-519
	public void fallsBackToRepositoryDomainTypeIfMethodDoesNotReturnADomainType() throws Exception {

		ReactiveElasticsearchQueryMethod method = queryMethod(PersonRepository.class, "deleteByName", String.class);

		assertThat(method.getEntityInformation().getJavaType()).isAssignableFrom(Person.class);
	}

	private ReactiveElasticsearchQueryMethod queryMethod(Class<?> repository, String name, Class<?>... parameters)
			throws Exception {

		Method method = repository.getMethod(name, parameters);
		ProjectionFactory factory = new SpelAwareProxyProjectionFactory();
		return new ReactiveElasticsearchQueryMethod(method, new DefaultRepositoryMetadata(repository), factory,
				mappingContext);
	}

	interface PersonRepository extends Repository<Person, String> {

		Mono<Person> findMonoByName(String name, Pageable pageRequest);

		Mono<Page<Person>> findMonoPageByName(String name, Pageable pageRequest);

		Mono<Slice<Person>> findMonoSliceByName(String name, Pageable pageRequest);

		Flux<Person> findByName(String name);

		Flux<Person> findByName(String name, Pageable pageRequest);

		void deleteByName(String name);
	}

	interface NonReactiveRepository extends Repository<Person, Long> {

		List<Person> method();
	}

}
