package net.lecousin.reactive.data.relational.schema;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.annotations.CompositeId;
import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import net.lecousin.reactive.data.relational.model.ModelUtils;

public class SchemaBuilderFromEntities {
	
	private LcReactiveDataRelationalClient client;
	private RelationalDatabaseSchema schema = new RelationalDatabaseSchema();
	
	public SchemaBuilderFromEntities(LcReactiveDataRelationalClient client) {
		this.client = client;
	}
	
	public RelationalDatabaseSchema getSchema() {
		return schema;
	}

	public RelationalDatabaseSchema build(Collection<Class<?>> entities) {
		for (Class<?> entity : entities) {
			schema.add(buildTable(entity));
		}
		return schema;
	}
	
	protected Table buildTable(Class<?> entity) {
		RelationalPersistentEntity<?> entityType = client.getMappingContext().getRequiredPersistentEntity(entity);
		Table table = new Table(entityType.getTableName().toSql(client.getDialect().getIdentifierProcessing()));
		for (RelationalPersistentProperty property : entityType)
			table.add(buildColumn(property));
		CompositeId compositeId = entityType.findAnnotation(CompositeId.class);
		if (compositeId != null) {
			Index index = new Index(compositeId.indexName());
			index.setUnique(true);
			for (String propertyName : compositeId.properties()) {
				RelationalPersistentProperty property = entityType.getRequiredPersistentProperty(propertyName);
				index.addColumn(property.getColumnName().toSql(client.getDialect().getIdentifierProcessing()));
			}
			table.add(index);
		}
		List<net.lecousin.reactive.data.relational.annotations.Index> indexes = new LinkedList<>();
		net.lecousin.reactive.data.relational.annotations.Index indexAnnotation = entityType.findAnnotation(net.lecousin.reactive.data.relational.annotations.Index.class);
		if (indexAnnotation != null)
			indexes.add(indexAnnotation);
		net.lecousin.reactive.data.relational.annotations.Indexes indexesAnnotation = entityType.findAnnotation(net.lecousin.reactive.data.relational.annotations.Indexes.class);
		if (indexesAnnotation != null)
			for (net.lecousin.reactive.data.relational.annotations.Index i : indexesAnnotation.value())
				indexes.add(i);
		for (net.lecousin.reactive.data.relational.annotations.Index i : indexes) {
			Index index = new Index(i.name());
			index.setUnique(i.unique());
			for (String propertyName : i.properties()) {
				RelationalPersistentProperty property = entityType.getRequiredPersistentProperty(propertyName);
				index.addColumn(property.getColumnName().toSql(client.getDialect().getIdentifierProcessing()));
			}
			table.add(index);
		}
		return table;
	}
	
	protected Column buildColumn(RelationalPersistentProperty property) {
		Column col = new Column(property.getColumnName().toSql(client.getDialect().getIdentifierProcessing()));
		if (property.isAnnotationPresent(Id.class))
			col.setPrimaryKey(true);
		col.setNullable(ModelUtils.isNullable(property));
		GeneratedValue generated = property.findAnnotation(GeneratedValue.class);
		if (generated != null) {
			col.setAutoIncrement(true);
		}
		Class<?> type = property.getType();
		if (property.isAnnotationPresent(ForeignKey.class)) {
			RelationalPersistentEntity<?> entity = client.getMappingContext().getRequiredPersistentEntity(type);
			RelationalPersistentProperty idProperty = entity.getRequiredIdProperty();
			type = idProperty.getType();
		}
		ColumnDefinition def = property.findAnnotation(ColumnDefinition.class);
		col.setType(client.getSchemaDialect().getColumnType(col, type, def));
		return col;
	}

}
