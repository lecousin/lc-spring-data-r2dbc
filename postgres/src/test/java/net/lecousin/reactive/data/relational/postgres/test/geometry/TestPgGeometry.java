package net.lecousin.reactive.data.relational.postgres.test.geometry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.test.context.ContextConfiguration;

import io.r2dbc.postgresql.codec.Box;
import io.r2dbc.postgresql.codec.Circle;
import io.r2dbc.postgresql.codec.Line;
import io.r2dbc.postgresql.codec.Lseg;
import io.r2dbc.postgresql.codec.Path;
import io.r2dbc.postgresql.codec.Point;
import io.r2dbc.postgresql.codec.Polygon;
import net.lecousin.reactive.data.relational.postgres.test.PostgresTestConfiguration;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepositoryFactoryBean;
import net.lecousin.reactive.data.relational.test.AbstractLcReactiveDataRelationalTest;

@ContextConfiguration(classes = { PostgresTestConfiguration.class })
@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class)
@ComponentScan
public class TestPgGeometry extends AbstractLcReactiveDataRelationalTest {

	@Autowired
	private PgGeometryRepository repo;
	
	@Test
	public void testGeometry() {
		PgGeometryEntity entity = new PgGeometryEntity();
		entity.setSomeText("hello");
		entity.setGeoPoint(Point.of(1.5d, 825.65d));
		entity.setGeoBox(Box.of(Point.of(1.d, 2.5d), Point.of(15.687d, 3.1d)));
		entity.setGeoCircle(Circle.of(16.45d, 86.39d, 14d));
		entity.setGeoLine(Line.of(3489.254d, -18.6d, 258.369d, 0d));
		entity.setGeoLseg(Lseg.of(-147d, 159.7536d, 20.20d, -98.7d));
		entity.setGeoPath(Path.closed(Point.of(3d, 1.56d), Point.of(3d, 2d), Point.of(3.6d, 1.888d)));
		entity.setGeoPolygon(Polygon.of(Point.of(10d, 20d), Point.of(15d, 25d), Point.of(12d, 22d)));
		repo.save(entity).block();
		
		entity = repo.findAll().blockFirst();
		Assertions.assertEquals("hello", entity.getSomeText());
		Assertions.assertEquals(Point.of(1.5d, 825.65d), entity.getGeoPoint());
		Assertions.assertEquals(Box.of(Point.of(1.d, 2.5d), Point.of(15.687d, 3.1d)), entity.getGeoBox());
		Assertions.assertEquals(Circle.of(16.45d, 86.39d, 14d), entity.getGeoCircle());
		Assertions.assertEquals(Line.of(3489.254d, -18.6d, 258.369d, 0d), entity.getGeoLine());
		Assertions.assertEquals(Lseg.of(-147d, 159.7536d, 20.20d, -98.7d), entity.getGeoLseg());
		Assertions.assertEquals(Path.closed(Point.of(3d, 1.56d), Point.of(3d, 2d), Point.of(3.6d, 1.888d)), entity.getGeoPath());
		Assertions.assertEquals(Polygon.of(Point.of(10d, 20d), Point.of(15d, 25d), Point.of(12d, 22d)), entity.getGeoPolygon());
	}

}
