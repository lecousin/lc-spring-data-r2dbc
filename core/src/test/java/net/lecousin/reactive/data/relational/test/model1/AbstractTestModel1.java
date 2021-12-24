package net.lecousin.reactive.data.relational.test.model1;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepositoryFactoryBean;
import net.lecousin.reactive.data.relational.test.AbstractLcReactiveDataRelationalTest;

@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class)
public abstract class AbstractTestModel1 extends AbstractLcReactiveDataRelationalTest {

	@Autowired
	private PersonRepository repoPerson;
	
	@Autowired
	private CompanyRepository repoCompany;
	
	@Override
	protected Collection<Class<?>> usedEntities() {
		return Arrays.asList(Company.class, Employee.class, Person.class, PointOfContact.class, PostalAddress.class, Site.class, User.class);
	}
	
	private void createModel() {
		Person johnSmith = createPerson("John", "Smith", null);
		Person joeSmith = createPerson("Joe", "Smith", createAddress("Street 5", 12345, "Madrid", "ES"));
		Person emilyTaylor = createPerson("Emily", "Taylor", null);
		Person jessicaTaylor = createPerson("Jessica", "Taylor", createAddress("Street 3", 1111, "Paris", "FR"));
		Person meganDavis = createPerson("Megan", "Davis", null);
		Person jamesMiller = createPerson("James", "Miller", null);
		
		Company google = createCompany("Google");
		Company apple = createCompany("Apple");
		Company microsoft = createCompany("Microsoft");
		
		addEmployee(google, johnSmith);
		addEmployee(google, joeSmith);
		addEmployee(apple, emilyTaylor);
		addEmployee(microsoft, jessicaTaylor);
		
		addSite(google, createAddress("Street 1", 12345, "Madrid", "ES"), "Google Spain");
		addSite(apple, createAddress("Street 5", 4587, "London", "UK"), "Apple UK");
		addSite(apple, createAddress("Street 1", 6982, "Paris", "FR"), "Apple FR");
		addSite(microsoft, createAddress("Street 2", 12345, "Madrid", "ES"), "Microsoft Spain");
		addSite(microsoft, createAddress("Street 3", 963852, "Marseille", "FR"), "Microsoft France");
		
		google.setProviders(new PointOfContact[] { createPOC(google, apple, meganDavis, "poc1") , createPOC(google, microsoft, jessicaTaylor, "poc2")});
		apple.setProviders(new PointOfContact[] { createPOC(apple, microsoft, jamesMiller, "poc3") });
		microsoft.setProviders(new PointOfContact[] { createPOC(microsoft, apple, jamesMiller, "poc4") });
		
		google.setOwner(meganDavis);
		apple.setOwner(jamesMiller);
		microsoft.setOwner(emilyTaylor);
		
		repoCompany.saveAll(Arrays.asList(google, apple, microsoft)).collectList().block();
		
		expectEntities(
			Company.class,
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Google"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Microsoft"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Apple"), new ExpectedValue<>(Company::getVersion, 1L))
		);
		expectEntities(
			Employee.class,
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "John")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Joe")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Emily")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Jessica"))
		);
		expectEntities(
			Person.class,
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "John"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Joe"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Emily"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Jessica"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Megan"), new ExpectedValue<>(Person::getLastName, "Davis"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "James"), new ExpectedValue<>(Person::getLastName, "Miller"), new ExpectedValue<>(Person::getVersion, 1))
		);
		expectEntities(
			PointOfContact.class,
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Megan"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Jessica"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "James"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "James"), new ExpectedValue<>(PointOfContact::getVersion, 1))
		);
		Assertions.assertEquals(7, SelectQuery.from(PostalAddress.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(5, SelectQuery.from(Site.class, "entity").execute(lcClient).collectList().block().size());
		
		Assertions.assertEquals(3, SelectQuery.from(Company.class, "company").join("company", "sites", "site").join("company", "employees", "employee").executeCount(lcClient).block());
	}
	
	private static Person createPerson(String firstName, String lastName, PostalAddress address) {
		Person p = new Person();
		p.setFirstName(firstName);
		p.setLastName(lastName);
		p.setAddress(address);
		return p;
	}
	
	private static Company createCompany(String name) {
		Company c = new Company();
		c.setName(name);
		return c;
	}
	
	private static PostalAddress createAddress(String street, int postalCode, String city, String country) {
		PostalAddress a = new PostalAddress();
		a.setAddress(street);
		a.setPostalCode(postalCode);
		a.setCity(city);
		a.setCountry(country);
		return a;
	}
	
	private static void addEmployee(Company c, Person p) {
		if (c.getEmployees() == null)
			c.setEmployees(new LinkedList<>());
		Employee e = new Employee();
		e.setCompany(c);
		e.setPerson(p);
		c.getEmployees().add(e);
	}
	
	private static void addSite(Company c, PostalAddress a, String name) {
		Site s = new Site();
		s.setCompany(c);
		s.setAddress(a);
		s.setName(name);
		if (c.getSites() == null)
			c.setSites(new LinkedList<>());
		c.getSites().add(s);
	}
	
	private static PointOfContact createPOC(Company owner, Company provider, Person person, String description) {
		PointOfContact poc = new PointOfContact();
		poc.setOwner(owner);
		poc.setPerson(person);
		poc.setProvider(provider);
		poc.setDescription(description);
		return poc;
	}
	
	@Test
	public void testLazyGetArray() {
		createModel();
		
		Company microsoft = repoCompany.findByName("Microsoft").block();
		Assertions.assertNull(microsoft.getProviders());
		Assertions.assertEquals(1, microsoft.lazyGetProviders().collectList().block().size());
		Assertions.assertEquals(1, microsoft.getProviders().length);
		Assertions.assertEquals(1, microsoft.lazyGetProviders().collectList().block().size());
		
		Company google = repoCompany.findByName("Google").block();
		Assertions.assertNull(google.getProviders());
		Assertions.assertEquals(2, google.lazyGetProviders().collectList().block().size());
		Assertions.assertEquals(2, google.getProviders().length);
		Assertions.assertEquals(2, google.lazyGetProviders().collectList().block().size());
	}
	
	@Test
	public void testDeleteEmployeeWithLazyLoadedPerson() {
		createModel();

		Company microsoft = repoCompany.findByName("Microsoft").block();
		Assertions.assertEquals(1, microsoft.lazyGetEmployees().collectList().block().size());
		Employee employee = microsoft.getEmployees().get(0);
		// person on employee is not loaded
		Assertions.assertNotNull(employee.getPerson());
		Assertions.assertFalse(employee.getPerson().entityLoaded());
		
		// delete employee
		lcClient.delete(employee).block();

		expectEntities(
			Company.class,
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Google"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Microsoft"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Apple"), new ExpectedValue<>(Company::getVersion, 1L))
		);
		expectEntities(
			Employee.class,
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "John")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Joe")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Emily"))
			//new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Jessica"))
		);
		expectEntities(
			Person.class,
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "John"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Joe"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Emily"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Jessica"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Megan"), new ExpectedValue<>(Person::getLastName, "Davis"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "James"), new ExpectedValue<>(Person::getLastName, "Miller"), new ExpectedValue<>(Person::getVersion, 1))
		);
		expectEntities(
			PointOfContact.class,
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Megan"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Jessica"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "James"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "James"), new ExpectedValue<>(PointOfContact::getVersion, 1))
		);
		Assertions.assertEquals(7, SelectQuery.from(PostalAddress.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(5, SelectQuery.from(Site.class, "entity").execute(lcClient).collectList().block().size());

		// Jessica has no more job
		List<Person> persons = repoPerson.findByFirstName("Jessica").collectList().block();
		Assertions.assertEquals(1, persons.size());
		Person jessica = persons.get(0);
		Assertions.assertNull(jessica.lazyGetJob().block());
		
		// Microsoft has no more employee
		microsoft = repoCompany.findByName("Microsoft").block();
		Assertions.assertEquals(0,  microsoft.lazyGetEmployees().collectList().block().size());
	}
	
	
	@Test
	public void testDeleteCompanies() {
		createModel();

		repoCompany.deleteAll().block();

		expectEntities(
			Company.class);
		expectEntities(
			Employee.class
		);
		expectEntities(
			Person.class,
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "John"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Joe"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Emily"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Jessica"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Megan"), new ExpectedValue<>(Person::getLastName, "Davis"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "James"), new ExpectedValue<>(Person::getLastName, "Miller"), new ExpectedValue<>(Person::getVersion, 1))
		);
		expectEntities(
			PointOfContact.class
		);
		Assertions.assertEquals(2, SelectQuery.from(PostalAddress.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(0, SelectQuery.from(Site.class, "entity").execute(lcClient).collectList().block().size());
	}
	
	@Test
	public void testDeleteCompany() {
		createModel();
		Company google = repoCompany.findByName("Google").block();
		repoCompany.delete(google).block();

		expectEntities(
			Company.class,
			//new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Google"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Microsoft"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Apple"), new ExpectedValue<>(Company::getVersion, 1L))
		);
		expectEntities(
			Employee.class,
			//new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "John")),
			//new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Joe")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Emily")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Jessica"))
		);
		expectEntities(
			Person.class,
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "John"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Joe"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Emily"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Jessica"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Megan"), new ExpectedValue<>(Person::getLastName, "Davis"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "James"), new ExpectedValue<>(Person::getLastName, "Miller"), new ExpectedValue<>(Person::getVersion, 1))
		);
		expectEntities(
			PointOfContact.class,
			//new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Megan"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			//new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Jessica"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "James"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "James"), new ExpectedValue<>(PointOfContact::getVersion, 1))
		);
		Assertions.assertEquals(7 - 1, SelectQuery.from(PostalAddress.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(5 - 1, SelectQuery.from(Site.class, "entity").execute(lcClient).collectList().block().size());
	}
	
	@Test
	public void testDeletePointOfContact() {
		createModel();
		
		Company microsoft = repoCompany.findByName("Microsoft").block();
		List<PointOfContact> pocs = microsoft.lazyGetProviders().collectList().block();
		Assertions.assertEquals(1, pocs.size());
		PointOfContact lazyPoc = SelectQuery.from(PointOfContact.class, "entity").where(Criteria.property("entity", "id").is(pocs.get(0).getId())).execute(lcClient).blockFirst();
		Assertions.assertNotNull(lazyPoc);
		lcClient.delete(lazyPoc).block();

		expectEntities(
			Company.class,
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Google"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Microsoft"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Apple"), new ExpectedValue<>(Company::getVersion, 1L))
		);
		expectEntities(
			Employee.class,
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "John")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Joe")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Emily")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Jessica"))
		);
		expectEntities(
			Person.class,
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "John"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Joe"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Emily"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Jessica"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Megan"), new ExpectedValue<>(Person::getLastName, "Davis"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "James"), new ExpectedValue<>(Person::getLastName, "Miller"), new ExpectedValue<>(Person::getVersion, 1))
		);
		expectEntities(
			PointOfContact.class,
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Megan"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Jessica"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "James"), new ExpectedValue<>(PointOfContact::getVersion, 1))
			//new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "James"), new ExpectedValue<>(PointOfContact::getVersion, 1))
		);
		Assertions.assertEquals(7, SelectQuery.from(PostalAddress.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(5, SelectQuery.from(Site.class, "entity").execute(lcClient).collectList().block().size());
	}
	
	@Test
	public void testDeletePointOfContactFromLoadedCompany() {
		createModel();
		
		Company microsoft = repoCompany.findByName("Microsoft").block();
		List<PointOfContact> pocs = microsoft.lazyGetProviders().collectList().block();
		Assertions.assertEquals(1, pocs.size());
		lcClient.delete(pocs.get(0)).block();
		
		Assertions.assertEquals(0, microsoft.getProviders().length);

		expectEntities(
			Company.class,
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Google"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Microsoft"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Apple"), new ExpectedValue<>(Company::getVersion, 1L))
		);
		expectEntities(
			Employee.class,
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "John")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Joe")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Emily")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Jessica"))
		);
		expectEntities(
			Person.class,
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "John"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Joe"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Emily"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Jessica"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Megan"), new ExpectedValue<>(Person::getLastName, "Davis"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "James"), new ExpectedValue<>(Person::getLastName, "Miller"), new ExpectedValue<>(Person::getVersion, 1))
		);
		expectEntities(
			PointOfContact.class,
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Megan"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Jessica"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "James"), new ExpectedValue<>(PointOfContact::getVersion, 1))
			//new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "James"), new ExpectedValue<>(PointOfContact::getVersion, 1))
		);
		Assertions.assertEquals(7, SelectQuery.from(PostalAddress.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(5, SelectQuery.from(Site.class, "entity").execute(lcClient).collectList().block().size());
	}
	
	@Test
	public void testDeleteSitesFromLoadedCompany() {
		createModel();
		
		Company microsoft = repoCompany.findByName("Microsoft").block();
		List<Site> sites = microsoft.lazyGetSites().collectList().block();
		Assertions.assertEquals(2, sites.size());
		Assertions.assertTrue(sites.stream().anyMatch(site -> "Microsoft Spain".equals(site.getName())));
		Assertions.assertTrue(sites.stream().anyMatch(site -> "Microsoft France".equals(site.getName())));
		lcClient.delete(sites).block();
		
		Assertions.assertEquals(0, microsoft.getSites().size());

		expectEntities(
			Company.class,
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Google"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Microsoft"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Apple"), new ExpectedValue<>(Company::getVersion, 1L))
		);
		expectEntities(
			Employee.class,
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "John")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Joe")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Emily")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Jessica"))
		);
		expectEntities(
			Person.class,
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "John"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Joe"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Emily"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Jessica"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Megan"), new ExpectedValue<>(Person::getLastName, "Davis"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "James"), new ExpectedValue<>(Person::getLastName, "Miller"), new ExpectedValue<>(Person::getVersion, 1))
		);
		expectEntities(
			PointOfContact.class,
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Megan"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Jessica"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "James"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "James"), new ExpectedValue<>(PointOfContact::getVersion, 1))
		);
		Assertions.assertEquals(7 - 2, SelectQuery.from(PostalAddress.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(5 - 2, SelectQuery.from(Site.class, "entity").execute(lcClient).collectList().block().size());
	}
	
	@Test
	public void testDeletePerson() {
		createModel();
		
		List<Person> persons = repoPerson.findByFirstName("Jessica").collectList().block();
		Assertions.assertEquals(1, persons.size());
		Person jessica = persons.get(0);
		repoPerson.delete(jessica).block();
		
		expectEntities(
			Company.class,
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Google"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Microsoft"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Apple"), new ExpectedValue<>(Company::getVersion, 1L))
		);
		expectEntities(
			Employee.class,
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "John")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Joe")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Emily"))
			//new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Jessica"))
		);
		expectEntities(
			Person.class,
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "John"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Joe"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Emily"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			//new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Jessica"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Megan"), new ExpectedValue<>(Person::getLastName, "Davis"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "James"), new ExpectedValue<>(Person::getLastName, "Miller"), new ExpectedValue<>(Person::getVersion, 1))
		);
		expectEntities(
			PointOfContact.class,
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson() != null ? e.getPerson().loadEntity().block().getFirstName() : null, "Megan"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson() != null ? e.getPerson().loadEntity().block().getFirstName() : null, null), new ExpectedValue<>(PointOfContact::getVersion, 2)), // version 2
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson() != null ? e.getPerson().loadEntity().block().getFirstName() : null, "James"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson() != null ? e.getPerson().loadEntity().block().getFirstName() : null, "James"), new ExpectedValue<>(PointOfContact::getVersion, 1))
		);
		Assertions.assertEquals(7 - 1, SelectQuery.from(PostalAddress.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(5, SelectQuery.from(Site.class, "entity").execute(lcClient).collectList().block().size());
	}
	
	@Test
	public void testDeletePersonNotLoaded() {
		createModel();
		
		Company microsoft = repoCompany.findByName("Microsoft").block();
		Person jessica = microsoft.lazyGetEmployees().collectList().block().get(0).getPerson();
		repoPerson.delete(jessica).block();
		
		expectEntities(
			Company.class,
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Google"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Microsoft"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Apple"), new ExpectedValue<>(Company::getVersion, 1L))
		);
		expectEntities(
			Employee.class,
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "John")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Joe")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Emily"))
			//new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Jessica"))
		);
		expectEntities(
			Person.class,
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "John"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Joe"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Emily"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			//new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Jessica"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Megan"), new ExpectedValue<>(Person::getLastName, "Davis"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "James"), new ExpectedValue<>(Person::getLastName, "Miller"), new ExpectedValue<>(Person::getVersion, 1))
		);
		expectEntities(
			PointOfContact.class,
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson() != null ? e.getPerson().loadEntity().block().getFirstName() : null, "Megan"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson() != null ? e.getPerson().loadEntity().block().getFirstName() : null, null), new ExpectedValue<>(PointOfContact::getVersion, 2)), // version 2
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson() != null ? e.getPerson().loadEntity().block().getFirstName() : null, "James"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson() != null ? e.getPerson().loadEntity().block().getFirstName() : null, "James"), new ExpectedValue<>(PointOfContact::getVersion, 1))
		);
		Assertions.assertEquals(7 - 1, SelectQuery.from(PostalAddress.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(5, SelectQuery.from(Site.class, "entity").execute(lcClient).collectList().block().size());
	}
	
	@Test
	public void testDeletePersonsNotLoaded() {
		createModel();
		
		Company google = repoCompany.findByName("Google").block();
		List<Person> persons = new LinkedList<>();
		for (Employee e : google.lazyGetEmployees().collectList().block())
			persons.add(e.getPerson());
		repoPerson.deleteAll(persons).block();
		
		expectEntities(
			Company.class,
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Google"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Microsoft"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Apple"), new ExpectedValue<>(Company::getVersion, 1L))
		);
		expectEntities(
			Employee.class,
			//new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "John")),
			//new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Joe")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Emily")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Jessica"))
		);
		expectEntities(
			Person.class,
			//new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "John"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			//new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Joe"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Emily"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Jessica"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Megan"), new ExpectedValue<>(Person::getLastName, "Davis"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "James"), new ExpectedValue<>(Person::getLastName, "Miller"), new ExpectedValue<>(Person::getVersion, 1))
		);
		expectEntities(
			PointOfContact.class,
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson() != null ? e.getPerson().loadEntity().block().getFirstName() : null, "Megan"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson() != null ? e.getPerson().loadEntity().block().getFirstName() : null, "Jessica"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson() != null ? e.getPerson().loadEntity().block().getFirstName() : null, "James"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson() != null ? e.getPerson().loadEntity().block().getFirstName() : null, "James"), new ExpectedValue<>(PointOfContact::getVersion, 1))
		);
		Assertions.assertEquals(7 - 1, SelectQuery.from(PostalAddress.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(5, SelectQuery.from(Site.class, "entity").execute(lcClient).collectList().block().size());
	}
	
	@Test
	public void testFindAllWithJoins() {
		createModel();
		
		List<Company> companies = repoCompany.findAllWithJoins().collectList().block();
		Assertions.assertEquals(3, companies.size());
		
		Company company;
		Person person;
		Site site;
		PointOfContact poc;
		
		// Google
		company = companies.stream().filter(e -> "Google".equals(e.getName())).findFirst().get();
		Assertions.assertEquals(2, company.getEmployees().size());
		// John is an employee
		person = company.getEmployees().stream().filter(e -> "John".equals(e.getPerson().getFirstName())).findFirst().get().getPerson();
		Assertions.assertNull(person.getAddress());
		Assertions.assertTrue(person.getJob().getCompany() == company);
		// Joe is an employee
		person = company.getEmployees().stream().filter(e -> "Joe".equals(e.getPerson().getFirstName())).findFirst().get().getPerson();
		Assertions.assertNotNull(person.getAddress());
		Assertions.assertEquals("Madrid", person.getAddress().getCity());
		Assertions.assertTrue(person.getJob().getCompany() == company);
		// 1 Site
		Assertions.assertEquals(1, company.getSites().size());
		site = company.getSites().get(0);
		Assertions.assertEquals("Google Spain", site.getName());
		Assertions.assertEquals("Madrid", site.getAddress().getCity());
		// 2 providers
		Assertions.assertEquals(2, company.getProviders().length);
		// Jessica is POC for Microsoft
		poc = Arrays.asList(company.getProviders()).stream().filter(e -> "Microsoft".equals(e.getProvider().getName())).findFirst().get();
		Assertions.assertTrue(poc.getOwner() == company);
		Assertions.assertEquals("Jessica", poc.getPerson().getFirstName());
		// Megan is POC for Apple
		poc = Arrays.asList(company.getProviders()).stream().filter(e -> "Apple".equals(e.getProvider().getName())).findFirst().get();
		Assertions.assertTrue(poc.getOwner() == company);
		Assertions.assertEquals("Megan", poc.getPerson().getFirstName());
		
		// Microsoft
		company = companies.stream().filter(e -> "Microsoft".equals(e.getName())).findFirst().get();
		Assertions.assertEquals(1, company.getEmployees().size());
		// Jessica is an employee
		person = company.getEmployees().stream().filter(e -> "Jessica".equals(e.getPerson().getFirstName())).findFirst().get().getPerson();
		Assertions.assertNotNull(person.getAddress());
		Assertions.assertEquals("Paris", person.getAddress().getCity());
		Assertions.assertTrue(person.getJob().getCompany() == company);
		// 2 Sites
		Assertions.assertEquals(2, company.getSites().size());
		site = company.getSites().stream().filter(e -> "Madrid".equals(e.getAddress().getCity())).findFirst().get();
		Assertions.assertEquals("Microsoft Spain", site.getName());
		site = company.getSites().stream().filter(e -> "Marseille".equals(e.getAddress().getCity())).findFirst().get();
		Assertions.assertEquals("Microsoft France", site.getName());
		// 1 providers
		Assertions.assertEquals(1, company.getProviders().length);
		// James is POC for Apple
		poc = Arrays.asList(company.getProviders()).stream().filter(e -> "Apple".equals(e.getProvider().getName())).findFirst().get();
		Assertions.assertTrue(poc.getOwner() == company);
		Assertions.assertEquals("James", poc.getPerson().getFirstName());
		
		// Apple
		company = companies.stream().filter(e -> "Apple".equals(e.getName())).findFirst().get();
		Assertions.assertEquals(1, company.getEmployees().size());
		// Emily is an employee
		person = company.getEmployees().stream().filter(e -> "Emily".equals(e.getPerson().getFirstName())).findFirst().get().getPerson();
		Assertions.assertNull(person.getAddress());
		Assertions.assertTrue(person.getJob().getCompany() == company);
		// 2 Sites
		Assertions.assertEquals(2, company.getSites().size());
		site = company.getSites().stream().filter(e -> "London".equals(e.getAddress().getCity())).findFirst().get();
		Assertions.assertEquals("Apple UK", site.getName());
		site = company.getSites().stream().filter(e -> "Paris".equals(e.getAddress().getCity())).findFirst().get();
		Assertions.assertEquals("Apple FR", site.getName());
		// 1 providers
		Assertions.assertEquals(1, company.getProviders().length);
		// James is POC for Microsoft
		poc = Arrays.asList(company.getProviders()).stream().filter(e -> "Microsoft".equals(e.getProvider().getName())).findFirst().get();
		Assertions.assertTrue(poc.getOwner() == company);
		Assertions.assertEquals("James", poc.getPerson().getFirstName());
		
		// with paging
		companies = repoCompany.findAllWithJoins(0, 2).collectList().block();
		Assertions.assertEquals(2, companies.size());
		companies = repoCompany.findAllWithJoins(1, 2).collectList().block();
		Assertions.assertEquals(2, companies.size());
		companies = repoCompany.findAllWithJoins(2, 2).collectList().block();
		Assertions.assertEquals(1, companies.size());
		companies = repoCompany.findAllWithJoins(3, 2).collectList().block();
		Assertions.assertEquals(0, companies.size());
		companies = repoCompany.findAllWithJoins(4, 2).collectList().block();
		Assertions.assertEquals(0, companies.size());
	}
	
	@Test
	public void testDeletePointOfContactFromLoadedEntities() {
		createModel();
		
		List<Company> companies = repoCompany.findAllWithJoins().collectList().block();
		Company company = companies.stream().filter(e -> "Google".equals(e.getName())).findFirst().get();
		company.setProviders(null);
		repoCompany.save(company).block();

		expectEntities(
			Company.class,
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Google"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Microsoft"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Apple"), new ExpectedValue<>(Company::getVersion, 1L))
		);
		expectEntities(
			Employee.class,
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "John")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Joe")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Emily")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Jessica"))
		);
		expectEntities(
			Person.class,
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "John"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Joe"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Emily"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Jessica"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Megan"), new ExpectedValue<>(Person::getLastName, "Davis"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "James"), new ExpectedValue<>(Person::getLastName, "Miller"), new ExpectedValue<>(Person::getVersion, 1))
		);
		expectEntities(
			PointOfContact.class,
			//new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Megan"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			//new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Jessica"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "James"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "James"), new ExpectedValue<>(PointOfContact::getVersion, 1))
		);
		Assertions.assertEquals(7, SelectQuery.from(PostalAddress.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(5, SelectQuery.from(Site.class, "entity").execute(lcClient).collectList().block().size());
	}
	
	@Test
	public void testRemoveOwningCompanyFromPerson() {
		createModel();
		
		List<Company> companies = repoCompany.findAllWithJoins().collectList().block();
		Company company = companies.stream().filter(e -> "Google".equals(e.getName())).findFirst().get();
		Person owner = company.getOwner();
		Assertions.assertNotNull(owner);
		Assertions.assertNotNull(owner.getOwningCompany());
		Assertions.assertTrue(company == owner.getOwningCompany());
		Assertions.assertEquals("Megan", owner.getFirstName());
		owner.setOwningCompany(null);
		repoPerson.save(owner).block();
		
		Assertions.assertNull(company.getOwner());
		companies = repoCompany.findAllWithJoins().collectList().block();
		company = companies.stream().filter(e -> "Google".equals(e.getName())).findFirst().get();
		Assertions.assertNull(company.getOwner());

		expectEntities(
			Company.class,
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Google"), new ExpectedValue<>(Company::getVersion, 1L + 1)), // version 2
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Microsoft"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Apple"), new ExpectedValue<>(Company::getVersion, 1L))
		);
		expectEntities(
			Employee.class,
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "John")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Joe")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Emily")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Jessica"))
		);
		expectEntities(
			Person.class,
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "John"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Joe"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Emily"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Jessica"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Megan"), new ExpectedValue<>(Person::getLastName, "Davis"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "James"), new ExpectedValue<>(Person::getLastName, "Miller"), new ExpectedValue<>(Person::getVersion, 1))
		);
		expectEntities(
			PointOfContact.class,
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Megan"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Jessica"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "James"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "James"), new ExpectedValue<>(PointOfContact::getVersion, 1))
		);
	}
	
	@Test
	public void testUniqueIndexes() {
		createModel();
		
		// try to create an already existing company
		Company google = createCompany("Google");
		try {
			repoCompany.save(google).block();
			throw new AssertionError("Error expected when creating a company with the same name");
		} catch (Exception e) {
			// expected
		}
		
		// try to create a person with already existing first name
		Person person = createPerson("John", "Smith2", null);
		repoPerson.save(person).block();

		// try to create a person with already existing last name
		person = createPerson("John2", "Smith", null);
		repoPerson.save(person).block();

		// try to create a person with already existing first name and last name
		person = createPerson("John", "Smith", null);
		try {
			repoPerson.save(person).block();
			throw new AssertionError("Error expected when creating a person with the same name");
		} catch (Exception e) {
			// expected
		}
	}
	
	@Test
	public void testRemovePersonFromUserAndUpdatePointOfContactInSameOperation() {
		createModel();
		
		// create user
		User user = new User();
		user.setUsername("abcd");
		user.setPerson(repoPerson.findByFirstName("Jessica").blockFirst());
		user = lcClient.save(user).block();
		
		// this will delete the person, which imply an update to null in the point of contact
		user.setPerson(null);
		// we do another change in the point of contact
		PointOfContact poc = SelectQuery.from(PointOfContact.class, "poc").where(Criteria.property("poc", "description").is("poc2")).execute(lcClient).blockFirst();
		poc.setDescription("poc2.2");
		poc.setPerson(null);
		
		lcClient.saveAll(user, poc).block();
		
		expectEntities(
			Company.class,
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Google"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Microsoft"), new ExpectedValue<>(Company::getVersion, 1L)),
			new ExpectedEntity<>(new ExpectedValue<>(Company::getName, "Apple"), new ExpectedValue<>(Company::getVersion, 1L))
		);
		expectEntities(
			Employee.class,
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "John")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Joe")),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Emily"))
			//new ExpectedEntity<>(new ExpectedValue<>(e -> e.getCompany().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson().loadEntity().block().getFirstName(), "Jessica"))
		);
		expectEntities(
			Person.class,
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "John"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Joe"), new ExpectedValue<>(Person::getLastName, "Smith"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Emily"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			//new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Jessica"), new ExpectedValue<>(Person::getLastName, "Taylor"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "Megan"), new ExpectedValue<>(Person::getLastName, "Davis"), new ExpectedValue<>(Person::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(Person::getFirstName, "James"), new ExpectedValue<>(Person::getLastName, "Miller"), new ExpectedValue<>(Person::getVersion, 1))
		);
		expectEntities(
			PointOfContact.class,
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson() != null ? e.getPerson().loadEntity().block().getFirstName() : null, "Megan"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Google"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson() != null ? e.getPerson().loadEntity().block().getFirstName() : null, null), new ExpectedValue<>(PointOfContact::getVersion, 2), new ExpectedValue<>(PointOfContact::getDescription, "poc2.2")), // version 2
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getPerson() != null ? e.getPerson().loadEntity().block().getFirstName() : null, "James"), new ExpectedValue<>(PointOfContact::getVersion, 1)),
			new ExpectedEntity<>(new ExpectedValue<>(e -> e.getOwner().loadEntity().block().getName(), "Microsoft"), new ExpectedValue<>(e -> e.getProvider().loadEntity().block().getName(), "Apple"), new ExpectedValue<>(e -> e.getPerson() != null ? e.getPerson().loadEntity().block().getFirstName() : null, "James"), new ExpectedValue<>(PointOfContact::getVersion, 1))
		);
	}

}
