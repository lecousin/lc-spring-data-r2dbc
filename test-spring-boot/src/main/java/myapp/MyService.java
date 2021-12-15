package myapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import myapp.model.MyEntity;
import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;

@Service
public class MyService {

	@Autowired
	private MyRepo repo;
	
	@Autowired
	private LcReactiveDataRelationalClient client;
	
	public boolean doSomething() {
		client.createSchemaContent(client.buildSchemaFromEntities()).block();
		MyEntity entity = new MyEntity();
		entity.setValue("Hello World !");
		repo.save(entity).block();
		return "Hello World !".equals(repo.findAll().blockFirst().getValue());
	}

}
