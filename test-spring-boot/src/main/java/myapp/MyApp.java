package myapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalInitializer;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepositoryFactoryBean;

@SpringBootApplication
@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class)
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
