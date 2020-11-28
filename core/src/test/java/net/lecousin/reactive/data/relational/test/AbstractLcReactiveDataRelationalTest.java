package net.lecousin.reactive.data.relational.test;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;

@DataR2dbcTest
@EnableAutoConfiguration
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class AbstractLcReactiveDataRelationalTest {
	
	@Autowired
	protected LcReactiveDataRelationalClient lcClient;
	
	@Autowired
	protected DatabaseClient springClient;
	
	@BeforeEach
	public void initDatabase() {
		lcClient.dropCreateTables().block();
	}
	
}
