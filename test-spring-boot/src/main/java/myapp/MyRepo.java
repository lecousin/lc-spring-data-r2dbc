package myapp;

import myapp.model.MyEntity;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepository;

public interface MyRepo extends LcR2dbcRepository<MyEntity, Long> {

}
