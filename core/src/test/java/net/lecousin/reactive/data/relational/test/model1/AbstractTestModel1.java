package net.lecousin.reactive.data.relational.test.model1;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.relational.core.query.Criteria;

import net.lecousin.reactive.data.relational.repository.LcR2dbcRepositoryFactoryBean;
import net.lecousin.reactive.data.relational.test.AbstractLcReactiveDataRelationalTest;

@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class)
public abstract class AbstractTestModel1 extends AbstractLcReactiveDataRelationalTest {

	@Autowired
	private PersonRepository repoPerson;
	
	@Autowired
	private CompanyRepository repoCompany;
	
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
		
		addSite(google, createAddress("Street 1", 12345, "Madrid", "ES"));
		addSite(apple, createAddress("Street 5", 4587, "London", "UK"));
		addSite(apple, createAddress("Street 1", 6982, "Paris", "FR"));
		addSite(microsoft, createAddress("Street 2", 12345, "Madrid", "ES"));
		addSite(microsoft, createAddress("Street 3", 963852, "Marseille", "FR"));
		
		google.setProviders(new PointOfContact[] { createPOC(google, apple, meganDavis) , createPOC(google, microsoft, jessicaTaylor)});
		apple.setProviders(new PointOfContact[] { createPOC(apple, microsoft, jamesMiller) });
		microsoft.setProviders(new PointOfContact[] { createPOC(microsoft, apple, jamesMiller) });
		
		repoCompany.saveAll(Arrays.asList(google, apple, microsoft)).collectList().block();
		
		Assertions.assertEquals(3, lcClient.getSpringClient().select().from(Company.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(4, lcClient.getSpringClient().select().from(Employee.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(6, lcClient.getSpringClient().select().from(Person.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(4, lcClient.getSpringClient().select().from(PointOfContact.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(7, lcClient.getSpringClient().select().from(PostalAddress.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(5, lcClient.getSpringClient().select().from(Site.class).fetch().all().collectList().block().size());
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
	
	private static void addSite(Company c, PostalAddress a) {
		Site s = new Site();
		s.setCompany(c);
		s.setAddress(a);
		if (c.getSites() == null)
			c.setSites(new LinkedList<>());
		c.getSites().add(s);
	}
	
	private static PointOfContact createPOC(Company owner, Company provider, Person person) {
		PointOfContact poc = new PointOfContact();
		poc.setOwner(owner);
		poc.setPerson(person);
		poc.setProvider(provider);
		return poc;
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

		Assertions.assertEquals(3, lcClient.getSpringClient().select().from(Company.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(4 - 1, lcClient.getSpringClient().select().from(Employee.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(6, lcClient.getSpringClient().select().from(Person.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(4, lcClient.getSpringClient().select().from(PointOfContact.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(7, lcClient.getSpringClient().select().from(PostalAddress.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(5, lcClient.getSpringClient().select().from(Site.class).fetch().all().collectList().block().size());

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
	public void testDeleteCompany() {
		createModel();
		Company google = repoCompany.findByName("Google").block();
		repoCompany.delete(google).block();

		Assertions.assertEquals(3 - 1, lcClient.getSpringClient().select().from(Company.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(4 - 2, lcClient.getSpringClient().select().from(Employee.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(6, lcClient.getSpringClient().select().from(Person.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(4 - 2, lcClient.getSpringClient().select().from(PointOfContact.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(7 - 1, lcClient.getSpringClient().select().from(PostalAddress.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(5 - 1, lcClient.getSpringClient().select().from(Site.class).fetch().all().collectList().block().size());
	}
	
	@Test
	public void testDeletePointOfContact() {
		createModel();
		
		Company microsoft = repoCompany.findByName("Microsoft").block();
		List<PointOfContact> pocs = microsoft.lazyGetProviders().collectList().block();
		Assertions.assertEquals(1, pocs.size());
		PointOfContact lazyPoc = lcClient.getSpringClient().select().from(PointOfContact.class).matching(Criteria.where("id").is(pocs.get(0).getId())).fetch().one().block();
		lcClient.delete(lazyPoc).block();

		Assertions.assertEquals(3, lcClient.getSpringClient().select().from(Company.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(4, lcClient.getSpringClient().select().from(Employee.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(6, lcClient.getSpringClient().select().from(Person.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(4 - 1, lcClient.getSpringClient().select().from(PointOfContact.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(7, lcClient.getSpringClient().select().from(PostalAddress.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(5, lcClient.getSpringClient().select().from(Site.class).fetch().all().collectList().block().size());
	}
	
	@Test
	public void testDeletePointOfContactFromLoadedCompany() {
		createModel();
		
		Company microsoft = repoCompany.findByName("Microsoft").block();
		List<PointOfContact> pocs = microsoft.lazyGetProviders().collectList().block();
		Assertions.assertEquals(1, pocs.size());
		lcClient.delete(pocs.get(0)).block();
		
		Assertions.assertEquals(0, microsoft.getProviders().length);

		Assertions.assertEquals(3, lcClient.getSpringClient().select().from(Company.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(4, lcClient.getSpringClient().select().from(Employee.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(6, lcClient.getSpringClient().select().from(Person.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(4 - 1, lcClient.getSpringClient().select().from(PointOfContact.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(7, lcClient.getSpringClient().select().from(PostalAddress.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(5, lcClient.getSpringClient().select().from(Site.class).fetch().all().collectList().block().size());
	}
	
	@Test
	public void testDeleteSitesFromLoadedCompany() {
		createModel();
		
		Company microsoft = repoCompany.findByName("Microsoft").block();
		List<Site> sites = microsoft.lazyGetSites().collectList().block();
		Assertions.assertEquals(2, sites.size());
		lcClient.delete(sites).block();
		
		Assertions.assertEquals(0, microsoft.getSites().size());

		Assertions.assertEquals(3, lcClient.getSpringClient().select().from(Company.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(4, lcClient.getSpringClient().select().from(Employee.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(6, lcClient.getSpringClient().select().from(Person.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(4, lcClient.getSpringClient().select().from(PointOfContact.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(7 - 2, lcClient.getSpringClient().select().from(PostalAddress.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(5 - 2, lcClient.getSpringClient().select().from(Site.class).fetch().all().collectList().block().size());
	}
	
	@Test
	public void testDeletePerson() {
		createModel();
		
		List<Person> persons = repoPerson.findByFirstName("Jessica").collectList().block();
		Assertions.assertEquals(1, persons.size());
		Person jessica = persons.get(0);
		repoPerson.delete(jessica).block();
		
		Assertions.assertEquals(3, lcClient.getSpringClient().select().from(Company.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(4 - 1, lcClient.getSpringClient().select().from(Employee.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(6 - 1, lcClient.getSpringClient().select().from(Person.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(4, lcClient.getSpringClient().select().from(PointOfContact.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(7 - 1, lcClient.getSpringClient().select().from(PostalAddress.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(5, lcClient.getSpringClient().select().from(Site.class).fetch().all().collectList().block().size());
	}
	
	@Test
	public void testDeletePersonNotLoaded() {
		createModel();
		
		Company microsoft = repoCompany.findByName("Microsoft").block();
		Person jessica = microsoft.lazyGetEmployees().collectList().block().get(0).getPerson();
		repoPerson.delete(jessica).block();
		
		Assertions.assertEquals(3, lcClient.getSpringClient().select().from(Company.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(4 - 1, lcClient.getSpringClient().select().from(Employee.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(6 - 1, lcClient.getSpringClient().select().from(Person.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(4, lcClient.getSpringClient().select().from(PointOfContact.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(7 - 1, lcClient.getSpringClient().select().from(PostalAddress.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(5, lcClient.getSpringClient().select().from(Site.class).fetch().all().collectList().block().size());
	}

}
