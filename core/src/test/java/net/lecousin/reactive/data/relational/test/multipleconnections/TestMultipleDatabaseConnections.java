package net.lecousin.reactive.data.relational.test.multipleconnections;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.R2dbcBadGrammarException;
import net.lecousin.reactive.data.relational.test.multipleconnections.db1.MultipleDbEntity1;
import net.lecousin.reactive.data.relational.test.multipleconnections.db1.MyRepository1;
import net.lecousin.reactive.data.relational.test.multipleconnections.db2.MultipleDbEntity2;
import net.lecousin.reactive.data.relational.test.multipleconnections.db2.MyRepository2;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {net.lecousin.reactive.data.relational.test.multipleconnections.db1.Config1.class, net.lecousin.reactive.data.relational.test.multipleconnections.db2.Config2.class})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class TestMultipleDatabaseConnections {

	public static interface DbUrls {
		@Bean
		@Qualifier("db1Url")
		String getFirstDbConnectionUrl();

		@Bean
		@Qualifier("db2Url")
		String getSecondDbConnectionUrl();
		
	}
	
	@Autowired
	private MyRepository1 repo1;
	
	@Autowired
	private MyRepository2 repo2;
	
	@Autowired
	@Qualifier("db1DatabaseConnectionFactory")
	private ConnectionFactory factory1;
	
	@Autowired
	@Qualifier("db2DatabaseConnectionFactory")
	private ConnectionFactory factory2;
	
	@Test
	public void test() {
		repo1.getLcClient().dropCreateSchemaContent(repo1.getLcClient().buildSchemaFromEntities(Arrays.asList(MultipleDbEntity1.class))).block();
		repo2.getLcClient().dropCreateSchemaContent(repo2.getLcClient().buildSchemaFromEntities(Arrays.asList(MultipleDbEntity2.class))).block();
		
		MultipleDbEntity1 e1 = new MultipleDbEntity1();
		e1.setValue("db1");
		repo1.save(e1).block();
		
		MultipleDbEntity2 e2 = new MultipleDbEntity2();
		e2.setValue("second db");
		repo2.save(e2).block();

		Assertions.assertEquals(1, repo1.count().block());
		Assertions.assertEquals("db1", repo1.findAll().next().block().getValue());

		Assertions.assertEquals(1, repo2.count().block());
		Assertions.assertEquals("second db", repo2.findAll().next().block().getValue());
		
		Assertions.assertNotEquals(repo1.getLcClient(), repo2.getLcClient());
		
		Assertions.assertEquals(1L, 
			Mono.from(factory1.create())
			.flatMap(connection ->
				Mono.from(connection.createStatement("SELECT COUNT(*) FROM multiple_db_entity1").execute())
				.flatMap(result -> Mono.from(result.map((r, m) -> r.get(0))))
				.doOnTerminate(() -> Mono.from(connection.close()).subscribe())
			).block()
		);
		
		Assertions.assertEquals(1L, 
			Mono.from(factory2.create())
			.flatMap(connection ->
				Mono.from(connection.createStatement("SELECT COUNT(*) FROM multiple_db_entity2").execute())
				.flatMap(result -> Mono.from(result.map((r, m) -> r.get(0))))
				.doOnTerminate(() -> Mono.from(connection.close()).subscribe())
			).block()
		);
		
		Assertions.assertThrows(R2dbcBadGrammarException.class, () -> 
			Mono.from(factory1.create())
			.flatMap(connection ->
				Mono.from(connection.createStatement("SELECT COUNT(*) FROM multiple_db_entity2").execute())
				.flatMap(result -> Mono.from(result.map((r, m) -> r.get(0))))
				.doOnTerminate(() -> Mono.from(connection.close()).subscribe())
			).block()
		);
		
		Assertions.assertThrows(R2dbcBadGrammarException.class, () -> 
			Mono.from(factory2.create())
			.flatMap(connection ->
				Mono.from(connection.createStatement("SELECT COUNT(*) FROM multiple_db_entity1").execute())
				.flatMap(result -> Mono.from(result.map((r, m) -> r.get(0))))
				.doOnTerminate(() -> Mono.from(connection.close()).subscribe())
			).block()
		);
	}

}
