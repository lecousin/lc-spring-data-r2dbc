/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.lecousin.reactive.data.relational.schema;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.util.Pair;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.annotations.CompositeId;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import net.lecousin.reactive.data.relational.model.metadata.EntityMetadata;
import net.lecousin.reactive.data.relational.model.metadata.PropertyMetadata;

/**
 * Build a {@link RelationalDatabaseSchema} from entity classes.
 * 
 * @author Guillaume Le Cousin
 *
 */
public class SchemaBuilderFromEntities {
	
	private SchemaBuilderFromEntities() {
		// no instance
	}
	
	public static RelationalDatabaseSchema build(Collection<EntityMetadata> entities) {
		RelationalDatabaseSchema schema = new RelationalDatabaseSchema();
		for (EntityMetadata entity : entities) {
			schema.add(buildTable(entity));
			addSequences(entity, schema);
		}
		for (EntityMetadata entity : entities) {
			addForeignKeys(entity, schema);
		}
		return schema;
	}
	
	protected static String getTableName(EntityMetadata entityType) {
		return entityType.getTableName().toSql(entityType.getClient().getDialect().getIdentifierProcessing());
	}
	
	protected static String getColumnName(PropertyMetadata property) {
		return property.getColumnName().toSql(property.getClient().getDialect().getIdentifierProcessing());
	}
	
	protected static Table buildTable(EntityMetadata entityType) {
		Table table = new Table(getTableName(entityType));
		for (PropertyMetadata property : entityType.getPersistentProperties())
			try {
				table.add(buildColumn(property));
			} catch (Exception e) {
				throw new MappingException("Error building schema for entity " + entityType.getName() + " on property " + property.getName(), e);
			}
		CompositeId compositeId = entityType.getCompositeIdAnnotation();
		if (compositeId != null) {
			Index index = new Index(compositeId.indexName());
			index.setUnique(true);
			for (String propertyName : compositeId.properties()) {
				PropertyMetadata property = entityType.getRequiredPersistentProperty(propertyName);
				index.addColumn(property.getColumnName().toSql(entityType.getClient().getDialect().getIdentifierProcessing()));
			}
			table.add(index);
		}
		List<net.lecousin.reactive.data.relational.annotations.Index> indexes = new LinkedList<>();
		net.lecousin.reactive.data.relational.annotations.Index indexAnnotation = entityType.getSpringMetadata().findAnnotation(net.lecousin.reactive.data.relational.annotations.Index.class);
		if (indexAnnotation != null)
			indexes.add(indexAnnotation);
		net.lecousin.reactive.data.relational.annotations.Indexes indexesAnnotation = entityType.getSpringMetadata().findAnnotation(net.lecousin.reactive.data.relational.annotations.Indexes.class);
		if (indexesAnnotation != null)
			Collections.addAll(indexes, indexesAnnotation.value());
		for (net.lecousin.reactive.data.relational.annotations.Index i : indexes) {
			Index index = new Index(i.name());
			index.setUnique(i.unique());
			for (String propertyName : i.properties()) {
				PropertyMetadata property = entityType.getRequiredPersistentProperty(propertyName);
				index.addColumn(getColumnName(property));
			}
			table.add(index);
		}
		return table;
	}
	
	protected static Column buildColumn(PropertyMetadata property) {
		Column col = new Column(property.getColumnName().toSql(property.getClient().getDialect().getIdentifierProcessing()));
		if (property.isId())
			col.setPrimaryKey(true);
		col.setNullable(property.isNullable());
		GeneratedValue generated = property.getGeneratedValueAnnotation();
		if (generated != null) {
			if (GeneratedValue.Strategy.AUTO_INCREMENT.equals(generated.strategy()))
				col.setAutoIncrement(true);
			else if (GeneratedValue.Strategy.RANDOM_UUID.equals(generated.strategy()))
				col.setRandomUuid(true);
		}
		Type type = property.getGenericType();
		if (property.isForeignKey()) {
			EntityMetadata entity = property.getClient().getRequiredEntity((Class<?>)type);
			PropertyMetadata idProperty = entity.getRequiredIdProperty();
			type = idProperty.getType();
		}
		ColumnDefinition def = property.getRequiredSpringProperty().findAnnotation(ColumnDefinition.class);
		col.setType(property.getClient().getSchemaDialect().getColumnType(col, type, def));
		return col;
	}
	
	protected static void addForeignKeys(EntityMetadata entityType, RelationalDatabaseSchema schema) {
		Iterator<PropertyMetadata> keys = entityType.getForeignKeys().iterator();
		if (!keys.hasNext())
			return;
		Table table = schema.getTable(getTableName(entityType));
		do {
			PropertyMetadata fkProperty = keys.next();
			Column fkColumn = table.getColumn(getColumnName(fkProperty));
			EntityMetadata foreignType = entityType.getClient().getRequiredEntity(fkProperty.getType());
			PropertyMetadata foreignId = foreignType.getRequiredIdProperty();
			Table foreignTable = schema.getTable(getTableName(foreignType));
			Column foreignColumn = foreignTable.getColumn(getColumnName(foreignId));
			fkColumn.setForeignKeyReferences(Pair.of(foreignTable, foreignColumn));
		} while (keys.hasNext());
	}
	
	protected static void addSequences(EntityMetadata entityType, RelationalDatabaseSchema schema) {
		for (PropertyMetadata property : entityType.getGeneratedValues()) {
			GeneratedValue annotation = property.getRequiredGeneratedValueAnnotation();
			if (annotation.strategy().equals(GeneratedValue.Strategy.SEQUENCE)) {
				Assert.isTrue(StringUtils.hasText(annotation.sequence()), "Sequence name must be specified");
				try {
					schema.getSequence(annotation.sequence());
					// already defined
				} catch (NoSuchElementException e) {
					schema.add(new Sequence(annotation.sequence()));
				}
			}
		}
	}

}
