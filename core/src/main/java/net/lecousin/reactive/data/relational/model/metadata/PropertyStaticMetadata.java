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
package net.lecousin.reactive.data.relational.model.metadata;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.annotations.JoinTable;
import net.lecousin.reactive.data.relational.enhance.Enhancer;
import net.lecousin.reactive.data.relational.model.ModelUtils;

/**
 * Static information (common for all database connections) about an entity property.
 * 
 * @author Guillaume Le Cousin
 *
 */
public class PropertyStaticMetadata {

	private static final String ATTRIBUTE1 = Enhancer.JOIN_TABLE_ATTRIBUTE_PREFIX + "1";
	private static final String ATTRIBUTE2 = Enhancer.JOIN_TABLE_ATTRIBUTE_PREFIX + "2";
	
	private Field field;

	private ForeignKey foreignKeyAnnotation;
	
	private ForeignTable foreignTableAnnotation;
	
	private JoinTable joinTableAnnotation;
	private PropertyStaticMetadata joinForeignTable;
	private String joinSourceFieldName;
	private String joinTargetFieldName;
	
	private boolean isCollection;
	private Class<?> collectionElementType;

	@SuppressWarnings("java:S3011")
	PropertyStaticMetadata(Field field) {
		this.field = field;
		field.setAccessible(true);
		foreignKeyAnnotation = field.getAnnotation(ForeignKey.class);
		foreignTableAnnotation = field.getAnnotation(ForeignTable.class);
		joinTableAnnotation = field.getAnnotation(JoinTable.class);
		isCollection = ModelUtils.isCollection(field);
		if (isCollection)
			collectionElementType = ModelUtils.getRequiredCollectionType(field);
	}
	
	void setJoinForeignTable(PropertyStaticMetadata joinForeignTable) {
		this.joinForeignTable = joinForeignTable;
		if (joinForeignTable.getForeignTableAnnotation().joinKey().equals(ATTRIBUTE1)) {
			joinSourceFieldName = ATTRIBUTE1;
			joinTargetFieldName = ATTRIBUTE2;
		} else {
			joinSourceFieldName = ATTRIBUTE2;
			joinTargetFieldName = ATTRIBUTE1;
		}
	}
	
	public @NonNull String getName() {
		return field.getName();
	}
	
	public @NonNull Field getField() {
		return field;
	}
	
	public boolean isCollection() {
		return isCollection;
	}
	
	public @NonNull Class<?> getType() {
		return field.getType();
	}
	
	public @NonNull Type getGenericType() {
		return field.getGenericType();
	}
	
	public @Nullable Class<?> getCollectionElementType() {
		return collectionElementType;
	}
	
	public @NonNull Class<?> getTypeOrCollectionElementType() {
		return isCollection ? collectionElementType : getType();
	}
	
	public boolean isForeignKey() {
		return foreignKeyAnnotation != null;
	}
	
	public ForeignKey getForeignKeyAnnotation() {
		return foreignKeyAnnotation;
	}
	
	public boolean isForeignTable() {
		return foreignTableAnnotation != null;
	}
	
	public ForeignTable getForeignTableAnnotation() {
		return foreignTableAnnotation;
	}
	
	public boolean isJoinTable() {
		return joinTableAnnotation != null;
	}
	
	public JoinTable getJoinTableAnnotation() {
		return joinTableAnnotation;
	}
	
	/** Returns the foreign table property generated for this join table property. */
	public PropertyStaticMetadata getJoinTableForeignTable() {
		return joinForeignTable;
	}
	
	public String getJoinTableSourceFieldName() {
		return joinSourceFieldName;
	}
	
	public String getJoinTableTargetFieldName() {
		return joinTargetFieldName;
	}
	
}
