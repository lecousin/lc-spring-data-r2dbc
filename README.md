# lc-spring-data-r2dbc

net.lecousin.reactive-data-relational
[![Maven Central](https://img.shields.io/maven-central/v/net.lecousin.reactive-data-relational/core.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22net.lecousin.reactive-data-relational%22%20AND%20a%3A%22core%22)
[![Javadoc](https://img.shields.io/badge/javadoc-0.4.2-brightgreen.svg)](https://www.javadoc.io/doc/net.lecousin.reactive-data-relational/core/0.4.2)
![Build status](https://github.com/lecousin/lc-spring-data-r2dbc/actions/workflows/maven.yml/badge.svg?branch=master)
[![Codecov](https://codecov.io/gh/lecousin/lc-spring-data-r2dbc/branch/master/graph/badge.svg)](https://codecov.io/gh/lecousin/lc-spring-data-r2dbc/branch/master)

The goal this library is to provide basic ORM features not covered by [Spring Data R2DBC](https://github.com/spring-projects/spring-data-r2dbc).

Spring Data R2DBC states that
> This is NOT an ORM 

> it does NOT offer caching, lazy loading, write behind or many other features of ORM frameworks. This makes Spring Data R2DBC a simple, limited, opinionated object mapper

In another hand, [Hibernate Reactive](https://github.com/hibernate/hibernate-reactive) is not integrated with Spring Data.

Waiting for a full ORM (like Hibernate), providing reactive streams to access to a relational database, and being integrated with Spring Data (with repositories...),
this library aims at providing the most useful features that are really missing from Spring Data R2DBC and that was provided by Spring Data JPA.

## Features

 - Lazy loading
 - Linked entities (1 to 1, 1 to n, n to 1, n to n)
 - Select statement with joins
 - Save (insert/update) with cascade
 - Delete with cascade
 - Composite Id
 - Sequence
 - Schema generation, with indexes, foreign key constraints, sequences

Features are detailed with examples in the [wiki section](https://github.com/lecousin/lc-spring-data-r2dbc/wiki)

## Supported databases

 - H2
 - Postgres
 - MySql 
 
## Dependencies

 - Spring Boot (org.springframework.boot:spring-boot-starter-data-r2dbc) version 2.5.6, Latest version: ![Maven Central](https://img.shields.io/maven-central/v/org.springframework.boot/spring-boot-starter-data-r2dbc.svg)
 - commons-lang3 version 3.12.0, Latest version: ![Maven Central](https://img.shields.io/maven-central/v/org.apache.commons/commons-lang3.svg)
 - javassist version 3.28.0-GA, Latest version: ![Maven Central](https://img.shields.io/maven-central/v/org.javassist/javassist.svg)

 - H2 (io.r2dbc:r2dbc-h2) version 0.8.4.RELEASE, Latest version: ![Maven Central](https://img.shields.io/maven-central/v/io.r2dbc/r2dbc-h2.svg)
 - MySql (dev.miku:r2dbc-mysql) version 0.8.2.RELEASE, Latest version: ![Maven Central](https://img.shields.io/maven-central/v/dev.miku/r2dbc-mysql.svg)
 - Postgres (io.r2dbc:r2dbc-postgresql) version 0.8.10.RELEASE, Latest version: ![Maven Central](https://img.shields.io/maven-central/v/io.r2dbc/r2dbc-postgresql.svg)

# Configuration

## Maven configuration

Add the Maven dependency, depending on your database:

### H2

```xml
<dependency>
  <groupId>net.lecousin.reactive-data-relational</groupId>
  <artifactId>h2</artifactId>
  <version>0.4.2</version>
</dependency>
```

### Postgres

```xml
<dependency>
  <groupId>net.lecousin.reactive-data-relational</groupId>
  <artifactId>postgres</artifactId>
  <version>0.4.2</version>
</dependency>
```

### MySql

```xml
<dependency>
  <groupId>net.lecousin.reactive-data-relational</groupId>
  <artifactId>mysql</artifactId>
  <version>0.4.2</version>
</dependency>
```

## Entities configuration: lc-reactive-data-relational.yaml

You have to declare the list of entity classes in a YAML resource file `lc-reactive-data-relational.yaml` (similar to file `persistence.xml` for JPA).
The classes are declared under the name `entities`, each level can declare a package, then leaf names are classes. For example:

```yaml
entities:
  - net.lecousin.reactive.data.relational.test:
    - simplemodel:
      - BooleanTypes
      - CharacterTypes
    - onetoonemodel:
      - MyEntity1
      - MySubEntity1
    - onetomanymodel:
      - RootEntity
      - SubEntity
      - SubEntity2
      - SubEntity3
```

This file is needed to *enhance* entity classes (using javassist) and support features such as lazy loading.

This enhancement must be done before any entity class is loaded, by using `LcReactiveDataRelationalInitializer.init()` at startup, as described in the next section.

## Spring Boot configuration

In your Spring Boot application class, you need to:
- add `@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class)`
- launch the initializer `LcReactiveDataRelationalInitializer.init()` that will load your `lc-reactive-data-relational.yaml` configuration file, before your application starts.

Example:

```java
@SpringBootApplication
@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class)
public class MyApp {

	public static void main(String[] args) {
		LcReactiveDataRelationalInitializer.init();
		SpringApplication.run(MyApp.class, args);
	}

}
```

The last step is to provide the R2DBC configuration with the connection to the database. Depending on the database type, you can use one of this configuration class:
 - `net.lecousin.reactive.data.relational.h2.H2Configuration`
 - `net.lecousin.reactive.data.relational.mysql.MySqlConfiguration`
 - `net.lecousin.reactive.data.relational.postgres.PostgresConfiguration`

### Using application properties

If you configure the connection using application properties, you just have to import the configuration class in your application class.
Example for H2:

```java
@Import(H2Configuration.class)
```

And configure the connection in application.properties or application.yml, using Spring R2DBC normal configuration:

```yaml
spring:
  r2dbc:
    username: sa
    url: r2dbc:h2:mem:///testdb;DB_CLOSE_DELAY=-1;
```

An example of a basic Spring Boot application is available [here](https://github.com/lecousin/lc-spring-data-r2dbc/tree/master/test-spring-boot)

### Using custom connection factory

Another way is to extend the configuration class to provide your own ConnectionFactory

Example for H2 in-memory database:

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import net.lecousin.reactive.data.relational.h2.H2Configuration;

@Configuration
public class H2TestConfiguration extends H2Configuration {

	@Override
	@Bean
	public H2ConnectionFactory connectionFactory() {
		return new H2ConnectionFactory(
			H2ConnectionConfiguration.builder()
			.url("mem:testdb;DB_CLOSE_DELAY=-1;")
			.username("sa")
			.build()
		);
	}
	
}
```

## JUnit 5

For your tests, using JUnit 5, you can use the annotation `@DataR2dbcTest` provided by Spring, and add the annotation `@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class)`.

In order to make sure the initializer is launched before any test class is loaded, add the following maven dependency, which will automatically call `LcReactiveDataRelationalInitializer.init()` to load your `lc-reactive-data-relational.yaml` configuration file during JUnit startup:

```xml
<dependency>
  <groupId>net.lecousin.reactive-data-relational</groupId>
  <artifactId>test-junit-5</artifactId>
  <version>0.4.2</version>
  <scope>test</scope>
</dependency>
```

# Usage

## Entity class

You can declare your entity classes in the same way as Spring Data R2DBC (with annotations `@Table` and `@Column`).
In addition, here are the following supported options:
- Annotation `org.springframework.data.annotation.Id` can be used to indicate a primary key
- Annotation `net.lecousin.reactive.data.relational.annotations.CompositeId` can be used to indicate properties to use as a unique key (when no @Id property is present)
- Annotation `net.lecousin.reactive.data.relational.annotations.GeneratedValue` can be used to indicate the value should be generated by the database (auto increment, using a sequence, or generating a random UUID)
- Annotation `org.springframework.data.annotation.Version` can be used for optimistic lock
- Annotation `net.lecousin.reactive.data.relational.annotations.ForeignKey` can be used to indicate a link. The cascade behavior can be specified using the attributes `optional`, `onForeignDeleted`, and `cascadeDelete`. A foreign key cannot be used on a collection type.
- Annotation `net.lecousin.reactive.data.relational.annotations.ForeignTable` is the other side of the foreign key. It does not store anything in the database
but indicates the link to another class. A foreign table can be used on a collection type for a one to many link.
- Annotation `net.lecousin.reactive.data.relational.annotations.JoinTable` can be used for a many to many (n-n) relationship when no additional field is required
on the join table. The join table will be automatically created with the 2 foreign keys. This allows to join directly between 2 tables with many to many relationship in a
transparent manner.
- Annotations `org.springframework.data.annotation.CreatedDate` and `org.springframework.data.annotation.LastModifiedDate` can be used to automatically store respectively the creation date and modification date. It can be used with a column of type `Long`, `Instant`, `LocalDate`, `LocalTime`, `OffsetTime`, `LocalDateTime`, or `ZonedDateTime`.

Additional methods may be declared in an Entity class to handle lazy loading, documented in the [dedicated section](#lazy-loading).

## Spring Repository

You can use Spring repositories as usual.
The methods to save and delete (such as `save`, `saveAll`, `delete`, `deleteAll`) will automatically be done with cascade.
The select methods (findXXX) do not perform any join by default, but allow lazy loading. However joins can be done using `SelectQuery`. This is described in the 2 following sections.

## Lazy loading

Spring Repository methods to find entities (`find...`) do not perform any join. If your class has links to other classes, they won't be loaded, however you can use lazy loading.

Lazy loading is done by declaring additional methods on your entity class, with a default body. **The body will be automatically replaced with the correct code**.

Here is an example of an entity enabling lazy loading:

```java
@Table
public class MyEntity {

	@Id @GeneratedValue
	private Long id;

	@ForeignKey(optional = false)
	private MyOtherEntity other;

	@ForeignTable(joinKey = "myEntity")
	private List<JoinEntity> links;

	/* The usual getters and setters. */

	public MyOtherEntity getOther() {
		return other;
	}
	
	public void setOther(MyOtherEntity other) {
		this.other = other;
	}
	
	public List<JoinEntity> getLinks() {
		return links;
	}

	public void setLinks(List<JoinEntity> links) {
		this.links = links;
	}
	
	/* Lazy loading attributes */

	public Mono<MyOtherEntity> lazyGetOther() {
		return null; // you can just return null, the body will be automatically generated by the enhancer
	}

	public Flux<JoinEntity> lazyGetLinks() {
		return null; // you can just return null, the body will be automatically generated by the enhancer
	}
	
	/* Lazy loading of this MyEntity */
	
	/** @return true if this MyEntity is loaded from database. */
	public boolean entityLoaded() {
		return false; // you can just return false, the body will be automatically generated by the enhancer
	}
	
	/** Ensure this MyEntity is loaded from database before to use. */
	public Mono<Entity1> loadEntity() {
		return null; // you can just return null, the body will be automatically generated by the enhancer
	}
	
}
```

As illustrated by this example, you can define the following methods to handle lazy loading on an entity class:
- methods `public Mono<T> lazyGetXXX() { return null; }` to get a loaded entity on a @ForeignKey or @ForeignTable field XXX.
- For collections, the same method can be declared with a `Flux`: `public Flux<T> lazyGetXXX() { return null; }` to get entities from a collection with @ForeignTable
- a method `public boolean entityLoaded() { return false; }` to know if the entity instance is loaded or not.
- a method `public Mono<T> loadEntity() { return null; }` where T is your class, to load the entity. If the entity is already loaded, `Mono.just(this)`is returned.

The body of those methods will be automatically generated during enhancement (when calling `LcReactiveDataRelationalInitializer.init()` at startup), so
you can just leave it as simple as possible.

Using methods `lazyGetXXX` or `loadEntity` allow to ensure the entity or attributes are loaded. If already loaded, nothing is done, else a database request is done. That's why
a `Mono` or `Flux` is returned, so you can perform your actions in a non-blocking mode even if a database request needs to be executed before.

In case an entity or attribute is not loaded, and you are not using those methods, here is the behavior you can expect:
- On a @ForeignKey attribute, a class will be instantiated with the foreign key as id, all other attributes are not set. That means your attribute is not null (using classical getter)
but all its attributes are null except its primary key which is pre-filled.
- On a @ForeignTable attribute, the attribute will be null.

Note that all those methods are completely optional. If you define a method the enhancer will generate its code, else the method will just not be available.

## Entity graph

Lazy loading is often not a good solution in term of performance, and we may want to load a graph of entities in a single database request using joins.

In JPA, entity graphs are used to specify the attributes to fetch automatically. This library does not provide similar way to indicate which links need to be loaded,
but you can use the class `SelectQuery` in your repositories to perform more complex searches and load linked entities.

For example:

```java
public interface RootEntityRepository extends LcR2dbcRepository<RootEntity, Long> {

	/** Search RootEntity having the given value, and fetch sub entities 'list'. */
	default Flux<RootEntity> findByValue(String value) {
		return SelectQuery
		.from(RootEntity.class, "entity")                      // SELECT entity FROM RootEntity AS entity
		.where(Criteria.property("entity", "value").is(value)) // WHERE entity.value = :value
		.join("entity", "list", "sub")                         // JOIN entity.list AS sub
		.execute(getLcClient());
	}
	
	/** Search RootEntity with a sub-entity having the given value, and fetch sub entities. */
	default Flux<RootEntity> findBySubValue(String value) {
		return SelectQuery
		.from(RootEntity.class, "entity")                      // SELECT entity FROM RootEntity AS entity
		.join("entity", "list", "sub")                         // JOIN entity.list AS sub
		.where(Criteria.property("sub", "subValue").is(value)) // WHERE sub.subValue = :value
		.execute(getLcClient());
	}

	/** Search RootEntity having the same value as one of its sub-entities. */	
	default Flux<RootEntity> havingSubValueEqualsToValue() {
		return SelectQuery
		.from(RootEntity.class, "entity")                      // SELECT entity FROM RootEntity AS entity
		.join("entity", "list", "sub")                         // JOIN entity.list AS sub
		.where(Criteria.property("sub", "subValue").is("entity", "value")) // WHERE sub.subValue = entity.value
		.execute(getLcClient());
	}
	
	/** Get all RootEntity and fetch links 'list', 'list2' and 'list3'. */
	default Flux<RootEntity> findAllFull() {
		return getLcClient().execute(
			SelectQuery.from(RootEntity.class, "root")         // SELECT root FROM RootEntity as root
			.join("root", "list", "sub1")                      // JOIN root.list as sub1
			.join("root", "list2", "sub2")                     // JOIN root.list2 as sub2
			.join("root", "list3", "sub3")                     // JOIN root.list3 as sub3
		);
	}

}
```

You can note the method `getLcClient()` needed to execute requests, which is automatically available if your repository extends `LcR2dbcRepository`.
If you don't need it, your repository can just extend the `R2dbcRepository` base interface of Spring.
