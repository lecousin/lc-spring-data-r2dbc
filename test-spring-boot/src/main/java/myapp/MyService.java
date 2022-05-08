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
		entity = repo.findAll().blockFirst();
		return entity != null && "Hello World !".equals(entity.getValue()) && entity.entityLoaded();
	}

}
