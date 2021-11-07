package myapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalInitializer;
import net.lecousin.reactive.data.relational.h2.H2Configuration;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepositoryFactoryBean;

@SpringBootApplication
@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class)
@Import(H2Configuration.class)
public class MyApp {

	public static void main(String[] args) {
		LcReactiveDataRelationalInitializer.init();
		try {
			ConfigurableApplicationContext app = SpringApplication.run(MyApp.class, args);
			if (app.getBean(MyService.class).doSomething())
				System.out.println("OK");
			else
				System.out.println("KO");
			app.close();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
		System.exit(0);
	}

}
