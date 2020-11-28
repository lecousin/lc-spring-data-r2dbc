package net.lecousin.reactive.data.relational.test.model1;

import java.util.Arrays;
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
		
		google.setProviders(new PointOfContact[] { createPOC(google, apple, meganDavis) , createPOC(google, microsoft, jessicaTaylor)});
		apple.setProviders(new PointOfContact[] { createPOC(apple, microsoft, jamesMiller) });
		microsoft.setProviders(new PointOfContact[] { createPOC(microsoft, apple, jamesMiller) });
		
		google.setOwner(meganDavis);
		apple.setOwner(jamesMiller);
		microsoft.setOwner(emilyTaylor);
		
		repoCompany.saveAll(Arrays.asList(google, apple, microsoft)).collectList().block();
		
		Assertions.assertEquals(3, SelectQuery.from(Company.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(4, SelectQuery.from(Employee.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(6, SelectQuery.from(Person.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(4, SelectQuery.from(PointOfContact.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(7, SelectQuery.from(PostalAddress.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(5, SelectQuery.from(Site.class, "entity").execute(lcClient).collectList().block().size());
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

		Assertions.assertEquals(3, SelectQuery.from(Company.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(4 - 1, SelectQuery.from(Employee.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(6, SelectQuery.from(Person.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(4, SelectQuery.from(PointOfContact.class, "entity").execute(lcClient).collectList().block().size());
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
	public void testDeleteCompany() {
		createModel();
		Company google = repoCompany.findByName("Google").block();
		repoCompany.delete(google).block();

		Assertions.assertEquals(3 - 1, SelectQuery.from(Company.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(4 - 2, SelectQuery.from(Employee.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(6, SelectQuery.from(Person.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(4 - 2, SelectQuery.from(PointOfContact.class, "entity").execute(lcClient).collectList().block().size());
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

		Assertions.assertEquals(3, SelectQuery.from(Company.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(4, SelectQuery.from(Employee.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(6, SelectQuery.from(Person.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(4 - 1, SelectQuery.from(PointOfContact.class, "entity").execute(lcClient).collectList().block().size());
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

		Assertions.assertEquals(3, SelectQuery.from(Company.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(4, SelectQuery.from(Employee.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(6, SelectQuery.from(Person.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(4 - 1, SelectQuery.from(PointOfContact.class, "entity").execute(lcClient).collectList().block().size());
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

		Assertions.assertEquals(3, SelectQuery.from(Company.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(4, SelectQuery.from(Employee.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(6, SelectQuery.from(Person.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(4, SelectQuery.from(PointOfContact.class, "entity").execute(lcClient).collectList().block().size());
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
		
		Assertions.assertEquals(3, SelectQuery.from(Company.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(4 - 1, SelectQuery.from(Employee.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(6 - 1, SelectQuery.from(Person.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(4, SelectQuery.from(PointOfContact.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(7 - 1, SelectQuery.from(PostalAddress.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(5, SelectQuery.from(Site.class, "entity").execute(lcClient).collectList().block().size());
	}
	
	@Test
	public void testDeletePersonNotLoaded() {
		createModel();
		
		Company microsoft = repoCompany.findByName("Microsoft").block();
		Person jessica = microsoft.lazyGetEmployees().collectList().block().get(0).getPerson();
		repoPerson.delete(jessica).block();
		
		Assertions.assertEquals(3, SelectQuery.from(Company.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(4 - 1, SelectQuery.from(Employee.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(6 - 1, SelectQuery.from(Person.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(4, SelectQuery.from(PointOfContact.class, "entity").execute(lcClient).collectList().block().size());
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

		Assertions.assertEquals(3, SelectQuery.from(Company.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(4, SelectQuery.from(Employee.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(6, SelectQuery.from(Person.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(4 - 2, SelectQuery.from(PointOfContact.class, "entity").execute(lcClient).collectList().block().size());
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
		owner.setOwningCompany(null);
		repoPerson.save(owner).block();
		
		Assertions.assertNull(company.getOwner());
		companies = repoCompany.findAllWithJoins().collectList().block();
		company = companies.stream().filter(e -> "Google".equals(e.getName())).findFirst().get();
		Assertions.assertNull(company.getOwner());
	}

}
