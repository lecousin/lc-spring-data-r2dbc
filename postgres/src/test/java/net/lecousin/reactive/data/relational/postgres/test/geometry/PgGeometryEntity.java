package net.lecousin.reactive.data.relational.postgres.test.geometry;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.r2dbc.postgresql.codec.Box;
import io.r2dbc.postgresql.codec.Circle;
import io.r2dbc.postgresql.codec.Line;
import io.r2dbc.postgresql.codec.Lseg;
import io.r2dbc.postgresql.codec.Path;
import io.r2dbc.postgresql.codec.Point;
import io.r2dbc.postgresql.codec.Polygon;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;

@Table
public class PgGeometryEntity {

	@Id @GeneratedValue
	private Long id;
	
	@Column
	private String someText;
	
	@Column
	private Point geoPoint;
	
	@Column
	private Box geoBox;
	
	@Column
	private Circle geoCircle;
	
	@Column
	private Line geoLine;
	
	@Column
	private Lseg geoLseg;
	
	@Column
	private Path geoPath;
	
	@Column
	private Polygon geoPolygon;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSomeText() {
		return someText;
	}

	public void setSomeText(String someText) {
		this.someText = someText;
	}

	public Point getGeoPoint() {
		return geoPoint;
	}

	public void setGeoPoint(Point geoPoint) {
		this.geoPoint = geoPoint;
	}

	public Box getGeoBox() {
		return geoBox;
	}

	public void setGeoBox(Box geoBox) {
		this.geoBox = geoBox;
	}

	public Circle getGeoCircle() {
		return geoCircle;
	}

	public void setGeoCircle(Circle geoCircle) {
		this.geoCircle = geoCircle;
	}

	public Line getGeoLine() {
		return geoLine;
	}

	public void setGeoLine(Line geoLine) {
		this.geoLine = geoLine;
	}

	public Lseg getGeoLseg() {
		return geoLseg;
	}

	public void setGeoLseg(Lseg geoLseg) {
		this.geoLseg = geoLseg;
	}

	public Path getGeoPath() {
		return geoPath;
	}

	public void setGeoPath(Path geoPath) {
		this.geoPath = geoPath;
	}

	public Polygon getGeoPolygon() {
		return geoPolygon;
	}

	public void setGeoPolygon(Polygon geoPolygon) {
		this.geoPolygon = geoPolygon;
	}

}
