package net.lecousin.reactive.data.relational.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class TestWrongConfiguration implements ApplicationContextAware {

	private ApplicationContext applicationContext;
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
	
	@Test
	void testConfigurationClassWithoutConnectionFactory() {
		MyWrongConfiguration cfg = new MyWrongConfiguration();
		
		Assertions.assertNull(cfg.connectionFactory());

		try {
			cfg.reactiveDataAccessStrategy(null);
			throw new AssertionError("Application context not initialized expected");
		} catch (Exception e) {
			Assertions.assertEquals("ApplicationContext is not yet initialized", e.getMessage());
		}

		cfg.setApplicationContext(applicationContext);


		try {
			cfg.reactiveDataAccessStrategy(null);
			throw new AssertionError("No connection factory error expected");
		} catch (Exception e) {
			Assertions.assertEquals("No r2dbc connection factory defined", e.getMessage());
		}

		Assertions.assertThrows(IllegalArgumentException.class, () -> cfg.r2dbcConverter(null, null));
	}

}
