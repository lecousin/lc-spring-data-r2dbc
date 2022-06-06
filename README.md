# lc-spring-data-r2dbc

<a href="https://search.maven.org/artifact/net.lecousin.reactive-data-relational/core"><img src="https://img.shields.io/maven-central/v/net.lecousin.reactive-data-relational/core.svg"></a> &nbsp;
<a href="https://www.javadoc.io/doc/net.lecousin.reactive-data-relational/core/0.10.2"><img src="https://img.shields.io/badge/javadoc-0.10.2-brightgreen.svg"></a> &nbsp;
<a href="https://github.com/lecousin/lc-spring-data-r2dbc/actions/workflows/maven.yml?query=branch%3Amaster"><img src="https://github.com/lecousin/lc-spring-data-r2dbc/actions/workflows/maven.yml/badge.svg?branch=master"></a>
<br/>
<a href="https://codecov.io/gh/lecousin/lc-spring-data-r2dbc/branch/master"><img src="https://codecov.io/gh/lecousin/lc-spring-data-r2dbc/branch/master/graph/badge.svg"></a> &nbsp;
<a href="https://sonarcloud.io/project/overview?id=lecousin_lc-spring-data-r2dbc"><img src="https://sonarcloud.io/api/project_badges/measure?project=lecousin_lc-spring-data-r2dbc&metric=coverage"></a> &nbsp;
<a href="https://sonarcloud.io/project/overview?id=lecousin_lc-spring-data-r2dbc"><img src="https://sonarcloud.io/api/project_badges/measure?project=lecousin_lc-spring-data-r2dbc&metric=bugs"></a> &nbsp;
<a href="https://sonarcloud.io/project/overview?id=lecousin_lc-spring-data-r2dbc"><img src="https://sonarcloud.io/api/project_badges/measure?project=lecousin_lc-spring-data-r2dbc&metric=vulnerabilities"></a> &nbsp;
<a href="https://sonarcloud.io/project/overview?id=lecousin_lc-spring-data-r2dbc"><img src="https://sonarcloud.io/api/project_badges/measure?project=lecousin_lc-spring-data-r2dbc&metric=code_smells"></a>


The goal this library is to provide basic ORM features not covered by Spring Data R2DBC (now part of [Spring Data Relational](https://github.com/spring-projects/spring-data-relational) project).

## Features

 - Lazy loading
 - Linked entities (1 to 1, 1 to n, n to 1, n to n)
 - Select statement with joins
 - Save (insert/update) with cascade
 - Delete with cascade
 - Composite Id
 - Sequence
 - Insert multiple rows in a single INSERT request (except for MySql)
 - Schema generation, with indexes, foreign key constraints, sequences
 - Array columns (only with Postgresql)

Features are detailed with examples in the [wiki section](https://github.com/lecousin/lc-spring-data-r2dbc/wiki)

## Supported databases

 - H2
 - Postgres
 - MySql 
 
## Dependencies version

<table>
	<tr>
		<th>Dependency</th>
		<th>groupId</th>
		<th>artifactId</th>
		<th>version</th>
		<th>Latest version</th>
	</tr>
	<tr>
		<td>Spring Boot</td>
		<td>org.springframework.boot</td>
		<td>spring-boot-starter-data-r2dbc</td>
		<td>2.6.7</td>
		<td><a href="https://search.maven.org/artifact/org.springframework.boot/spring-boot-starter-data-r2dbc"><img src="https://img.shields.io/maven-central/v/org.springframework.boot/spring-boot-starter-data-r2dbc.svg"</a></td>
	</tr>
	<tr>
		<td>Appache Commons Lang</td>
		<td>org.apache.commons</td>
		<td>commons-lang3</td>
		<td>3.12.0</td>
		<td><a href="https://search.maven.org/artifact/org.apache.commons/commons-lang3"><img src="https://img.shields.io/maven-central/v/org.apache.commons/commons-lang3.svg"</a></td>
	</tr>
	<tr>
		<td>Javassist</td>
		<td>org.javassist</td>
		<td>javassist</td>
		<td>3.28.0-GA</td>
		<td><a href="https://search.maven.org/artifact/org.javassist/javassist"><img src="https://img.shields.io/maven-central/v/org.javassist/javassist.svg"</a></td>
	</tr>
	<tr>
		<td>H2 driver</td>
		<td>io.r2dbc</td>
		<td>r2dbc-h2</td>
		<td>0.9.1.RELEASE</td>
		<td><a href="https://search.maven.org/artifact/io.r2dbc/r2dbc-h2"><img src="https://img.shields.io/maven-central/v/io.r2dbc/r2dbc-h2.svg"</a></td>
	</tr>
	<tr>
		<td>MySql driver</td>
		<td>dev.miku</td>
		<td>r2dbc-mysql</td>
		<td>0.8.2.RELEASE</td>
		<td><a href="https://search.maven.org/artifact/dev.miku/r2dbc-mysql"><img src="https://img.shields.io/maven-central/v/dev.miku/r2dbc-mysql.svg"</a></td>
	</tr>
	<tr>
		<td>PostgreSQL driver</td>
		<td>org.postgresql</td>
		<td>r2dbc-postgresql</td>
		<td>0.9.1.RELEASE</td>
		<td><a href="https://search.maven.org/artifact/org.postgresql/r2dbc-postgresql"><img src="https://img.shields.io/maven-central/v/org.postgresql/r2dbc-postgresql.svg"</a></td>
	</tr>
</table>

# Configuration

## Dependency configuration

Add the dependency to your project, depending on your database (you may add several if you are using multiple databases in your project):

### H2

Maven
```xml
<dependency>
  <groupId>net.lecousin.reactive-data-relational</groupId>
  <artifactId>h2</artifactId>
  <version>0.10.2</version>
</dependency>
```

Gradle
```groovy
implementation group: 'net.lecousin.reactive-data-relational', name: 'h2', version: '0.10.2'
```

### Postgres

Maven
```xml
<dependency>
  <groupId>net.lecousin.reactive-data-relational</groupId>
  <artifactId>postgres</artifactId>
  <version>0.10.2</version>
</dependency>
```

Gradle
```groovy
implementation group: 'net.lecousin.reactive-data-relational', name: 'postgres', version: '0.10.2'
```

### MySql

Maven
```xml
<dependency>
  <groupId>net.lecousin.reactive-data-relational</groupId>
  <artifactId>mysql</artifactId>
  <version>0.10.2</version>
</dependency>
```

Gradle
```groovy
implementation group: 'net.lecousin.reactive-data-relational', name: 'mysql', version: '0.10.2'
```

## Spring Boot configuration

In your Spring Boot application class, you need to:
- add `@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class)`
- launch the initializer `LcReactiveDataRelationalInitializer.init()` that will add functionalities to your entity classes, before your application starts. This step **MUST** be done before Spring starts to ensure no entity class is loaded yet in the JVM.
- configure your database

Example:

```java
@SpringBootApplication
@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class)
@Import(PostgresConfiguration.class) // here you can change depending on the database you are using
public class MyApp {

	public static void main(String[] args) {
		LcReactiveDataRelationalInitializer.init();
		SpringApplication.run(MyApp.class, args);
	}

}
```

The `@Import` annotation is used when using a single database connection, and your connection is configured through application properties (`application.properties` or `application.yml`). Depending on your database, you can use one of this configuration class:
 - `net.lecousin.reactive.data.relational.h2.H2Configuration`
 - `net.lecousin.reactive.data.relational.mysql.MySqlConfiguration`
 - `net.lecousin.reactive.data.relational.postgres.PostgresConfiguration`

Finally, configure how to connect to the database using Spring R2DBC normal configuration, here is an example of application.yml file:

```yaml
spring:
  r2dbc:
    username: sa
    url: r2dbc:h2:mem:///testdb;DB_CLOSE_DELAY=-1;
```

### Using custom connection factory

If you want to configure the connection programmatically instead of using application properties,
instead of using `@Import` annotation like in the previous example, you can extend the configuration class to provide your own `ConnectionFactory`:
 
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

### Multiple databases

If you need to connect to multiple databases, the configuration is different. You need to create a `@Configuration` class for each database connection, extending class `LcR2dbcEntityOperationsBuilder`. Instead of declaring `@EnableR2dbcRepositories` directly on your application class, you will declare it to each configuration class.

Here is an example of such a configuration class:

```java
@Configuration
@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class, basePackages = "com.example.book.dao.repository", entityOperationsRef = "bookOperations")
public class BookConfig extends LcR2dbcEntityOperationsBuilder {

	/** Connection factory. */
	@Bean
	@Qualifier("bookDatabaseConnectionFactory")
	public ConnectionFactory bookDatabaseConnectionFactory(@Value("${database.book}") String databaseUrl) {
		return ConnectionFactories.get(databaseUrl);
	}
	
	/** Entity operations bean. */
	@Bean
	@Qualifier("bookOperations")
	public LcR2dbcEntityTemplate bookOperations(@Qualifier("bookDatabaseConnectionFactory") ConnectionFactory connectionFactory) {
		return buildEntityOperations(connectionFactory);
	}

}
```

- define a bean to create a `ConnectionFactory` (here we get a url from the application configuration, but you can create it in another way)
- define a bean `LcR2dbcEntityTemplate` with the connection factory as argument
- add the annotation `@EnableR2dbcRepositories` with the packages containing the repositories that will use this database, and the attribute `entityOperationsRef` set to the qualifier of the `LcR2dbcEntityTemplate` bean

A complete example illustrating a Spring Boot application connecting to different databases is available in the repository [lc-spring-data-r2dbc-sample](https://github.com/lecousin/lc-spring-data-r2dbc-sample).


### Application startup time

By default, when calling `LcReactiveDataRelationalInitializer.init()`, all classes present in the class path are analyzed to find entity classes.
This can take some time especially if you have many libraries in your class path.

This behavior allows no additional configuration, which make it easier during development. However for production, if it is important for your application to startup faster
(for example a microservice), you can declare the list of entity classes in a YAML resource file `lc-reactive-data-relational.yaml` (similar to file `persistence.xml` for JPA).
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

The presence of this file will disable class path analysis. Note that you may have several resource files `lc-reactive-data-relational.yaml` in your class path (for example if several modules are providing entities), and all will be processed.

## JUnit 5

For your tests, using JUnit 5, you can use the annotation `@DataR2dbcTest` provided by Spring, and add the annotation `@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class)`.

In order to make sure the initializer is launched before any test class is loaded, add the following maven dependency, which will automatically call `LcReactiveDataRelationalInitializer.init()` during JUnit startup:

```xml
<dependency>
  <groupId>net.lecousin.reactive-data-relational</groupId>
  <artifactId>test-junit-5</artifactId>
  <version>0.10.2</version>
  <scope>test</scope>
</dependency>
```

# Usage

A more complete guide is available in the [wiki section](https://github.com/lecousin/lc-spring-data-r2dbc/wiki), here is an overview.

## Entity class

Your entity classes are declared using the Spring Data R2DBC annotations `@Table` and `@Column`:

```java
@Table
public class MyEntity {
	@Column
	private String myText;
}
```

In addition, here are the following supported annotations:
- `org.springframework.data.annotation.Id` indicates a primary key
- `net.lecousin.reactive.data.relational.annotations.CompositeId` indicates properties to use as a unique key (cannot be used together with @Id)
- `net.lecousin.reactive.data.relational.annotations.GeneratedValue` indicates a value that should be generated by the database (auto increment, using a sequence, or generating a random UUID)
- `org.springframework.data.annotation.Version` is used for optimistic lock
- `net.lecousin.reactive.data.relational.annotations.ForeignKey` indicates a link with a foreign key stored in the table. The cascade behavior can be specified using the attributes `optional`, `onForeignDeleted`, and `cascadeDelete`. The type of the attribute in the class will be the linked entity class (not the foreign key itself). A foreign key cannot be used on a collection type.
- `net.lecousin.reactive.data.relational.annotations.ForeignTable` is the other side of the foreign key. It does not store anything in the table
but indicates the link to another class to use with joins or lazy loading. A foreign table can be used on a collection type for a one to many link.
- `net.lecousin.reactive.data.relational.annotations.JoinTable` can be used for a many to many (n-n) relationship when no additional field is required
on the join table. The join table will be automatically created with the 2 foreign keys. This allows to join directly between 2 tables with many to many relationship in a
transparent manner.
- `org.springframework.data.annotation.CreatedDate` and `org.springframework.data.annotation.LastModifiedDate` can be used to automatically store respectively the creation date and modification date. It can be used with a column of type `Long`, `Instant`, `LocalDate`, `LocalTime` or `LocalDateTime`. `OffsetTime` and `ZonedDateTime` can be used except for MySql that does not support columns with timezone information.
- `net.lecousin.reactive.data.relational.annotations.ColumnDefinition` allows to specify constraints for schema generation.

Additional methods may be declared in an Entity class to handle lazy loading, documented in the [dedicated section](#lazy-loading).

Example:

```java
@Table
public class MyEntity {
	@Id @GeneratedValue
	private Long id;
	
	@ColumnDefinition(max = 100, nullable = true)
	private String someOptionalText;
	
	@ForeignKey(optional = false)
	private MyOtherEntity linkedEntity;
	
	[...]
}
```

### Important note for Java > 11

If you use a Java version > 11, the JVM does not allow to modify classes in another package, and this library won't be able to enhance your classes to provide all the functionalities. As a workaround for now, you must explicitly allow it by placing an empty interface named `AllowEnhancer` in **every package containing entities**.

You can just create it like this:

```java
public interface AllowEnhancer {
}
```

## Spring Repository

You can use Spring repositories as usual.
The methods to save and delete (such as `save`, `saveAll`, `delete`, `deleteAll`) will automatically be done with cascade.
The select methods (findXXX) do not perform any join by default, but allow lazy loading. However joins can be done using `SelectQuery`. This is described in the 2 following sections.

## Lazy loading

Spring Repository methods to find entities (`find...`) do not perform any join. If your class has links to other classes, they won't be loaded, however this library supports
_lazy loading_ 
which will load the linked entities on demand.

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
	
	/* Lazy loading methods. */

	public Mono<MyOtherEntity> lazyGetOther() {
		return null; // you can just return null, the correct code will be automatically generated.
	}

	public Flux<JoinEntity> lazyGetLinks() {
		return null; // you can just return null, the correct code will be automatically generated.
	}
	
	/* Lazy loading of this MyEntity (this class). */
	
	/** @return true if this MyEntity is loaded from database, false if only the @Id is available. */
	public boolean entityLoaded() {
		return false; // you can just return false, the correct code will be automatically generated.
	}
	
	/** Ensure this MyEntity is loaded from database before to use. */
	public Mono<MyEntity> loadEntity() {
		return null; // you can just return null, the correct code will be automatically generated.
	}
	
}
```

As illustrated by this example, you can define the following methods to handle lazy loading on an entity class:
- methods `public Mono<T> lazyGetXXX() { return null; }` to get a loaded entity on a @ForeignKey or @ForeignTable field XXX.
- For collections, the same method can be declared with a `Flux`: `public Flux<T> lazyGetXXX() { return null; }` to get entities from a collection with @ForeignTable
- a method `public boolean entityLoaded() { return false; }` to know if the entity instance is fully loaded or not.
- a method `public Mono<T> loadEntity() { return null; }` where T is your entity class, to load the instance from the database. If the entity is already loaded, `Mono.just(this)` is returned (no redundant database query on each call).

The body of those methods will be automatically generated during enhancement (when calling `LcReactiveDataRelationalInitializer.init()` at startup), that's why you can just return null or false in your code.

Using methods `lazyGetXXX` or `loadEntity` allow to ensure the entity or attributes are loaded. If already loaded, nothing is done, else a database request is done. That's why
a `Mono` or `Flux` is returned, so you can perform your actions in a non-blocking mode even if a database request needs to be executed.

In case an entity or attribute is not loaded, and you are not using those methods, here is the behavior you can expect:
- On a @ForeignKey attribute, a class will be instantiated with the foreign key as id, all other attributes are not set. That means your attribute is not null (using classical getter)
but all its attributes are null except its primary key which is pre-filled. In other words you can get the foreign key value without needing to load the full entity.
- On a @ForeignTable attribute, the attribute will be null. The reason is that the database table of your entity does not store any information about this link (unlike when having a foreign key).

Note that all those methods are completely optional. If you define some of those methods, the enhancer will generate the corresponding code, else the method will just not be available.

## Select with joins

Lazy loading is often not a good solution in term of performance, and we may want to load linked entities in a single database request using joins.

In JPA, entity graphs are used to specify the attributes to fetch automatically. This library does not provide similar way to indicate which links need to be loaded,
but you can use the class `SelectQuery` to perform more complex searches and load linked entities.

The `SelectQuery` can be used anywhere, including in your Spring Data repositories as default method (note that default methods in Spring Data repositories is not supported with Kotlin).

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
	
	/** Count RootEntity with a sub-entity having the given value. */
	default Mono<Long> findBySubValue(String value) {
		return SelectQuery
		.from(RootEntity.class, "entity")                      // SELECT entity FROM RootEntity AS entity
		.join("entity", "list", "sub")                         // JOIN entity.list AS sub
		.where(Criteria.property("sub", "subValue").is(value)) // WHERE sub.subValue = :value
		.executeCount(getLcClient());
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

`SelectQuery` can be used anywhere, not only in a repository, in this case you will need the client instance that you can inject in your class:

```java
	@Autowired
	private LcReactiveDataRelationalClient lcClient;
```

In case you have multiple database connections, you can inject the entity template configured for a specific database connection, then get the client using `template.getLcClient()`:

```java
	@Autowired
	@Qualifier("bookOperations")
	private LcR2dbcEntityTemplate template;
```

# Resources

- [Spring Data R2DBC documentation](https://spring.io/projects/spring-data-r2dbc) 

- [Wiki](https://github.com/lecousin/lc-spring-data-r2dbc/wiki)

- Example application: A complete example illustrating a Spring Boot application connecting to different databases is available in the repository [lc-spring-data-r2dbc-sample](https://github.com/lecousin/lc-spring-data-r2dbc-sample), and comes with an Angular GUI to test it.

# License

lc-spring-data-r2dbc is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0.html).

# Contributing

Any contribution is welcome !

Do not hesitate to start a [discussion](https://github.com/lecousin/lc-spring-data-r2dbc/discussions) about new features you would like to contribute to, or to open [an issue](https://github.com/lecousin/lc-spring-data-r2dbc/issues) if you encounter a bug.

Contributions to improve the documentation are also welcome.

