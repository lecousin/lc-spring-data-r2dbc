package net.lecousin.reactive.data.relational.test.simplemodel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TransactionalService {

	@Autowired
	private LcReactiveDataRelationalClient lcClient;
	
	@Transactional
	public Mono<CharacterTypes> createCorrectEntity() {
		CharacterTypes e = new CharacterTypes();
		e.setFixedLengthString("abcde");
		e.setLongString("Hello World");
		return lcClient.save(e);
	}
	
	@Transactional
	public Flux<CharacterTypes> createCorrectEntityThenInvalidEntity() {
		CharacterTypes e = new CharacterTypes();
		e.setFixedLengthString("abcde");
		e.setLongString("Hello World");
		Mono<CharacterTypes> save1 = lcClient.save(e);
		e = new CharacterTypes();
		Mono<CharacterTypes> save2 = lcClient.save(e);
		return Flux.concat(save1, save2);
	}
	
	@Transactional
	public Mono<Void> deleteEntity(CharacterTypes e) {
		return lcClient.delete(e);
	}

}
