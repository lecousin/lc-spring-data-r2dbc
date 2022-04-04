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
package net.lecousin.reactive.data.relational.query.criteria;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.SimpleFunction;
import org.springframework.data.relational.core.sql.Table;

import net.lecousin.reactive.data.relational.model.metadata.EntityInstance;
import net.lecousin.reactive.data.relational.model.metadata.EntityMetadata;
import net.lecousin.reactive.data.relational.model.metadata.PropertyMetadata;
import net.lecousin.reactive.data.relational.query.SqlQuery;
import net.lecousin.reactive.data.relational.query.criteria.Criteria.And;
import net.lecousin.reactive.data.relational.query.criteria.Criteria.Or;
import net.lecousin.reactive.data.relational.query.criteria.Criteria.PropertyOperand;
import net.lecousin.reactive.data.relational.query.criteria.Criteria.PropertyOperation;
import net.lecousin.reactive.data.relational.schema.dialect.RelationalDatabaseSchemaDialect.SqlFunction;

/**
 * Build SQL where clause from a Criteria.
 * 
 * @author Guillaume Le Cousin
 *
 */
public class CriteriaSqlBuilder implements CriteriaVisitor<Condition> {
	
	protected Map<String, EntityMetadata> entitiesByAlias;
	protected Map<String, Table> tablesByAlias;
	protected SqlQuery<?> query;

	public CriteriaSqlBuilder(Map<String, EntityMetadata> entitiesByAlias, Map<String, Table> tablesByAlias, SqlQuery<?> query) {
		this.entitiesByAlias = entitiesByAlias;
		this.tablesByAlias = tablesByAlias;
		this.query = query;
	}
	
	@Override
	public Condition visit(And and) {
		return and.getLeft().accept(this).and(and.getRight().accept(this));
	}
	
	@Override
	public Condition visit(Or or) {
		return or.getLeft().accept(this).or(or.getRight().accept(this));
	}
	
	@SuppressWarnings({"incomplete-switch", "java:S1301", "java:S131"})
	@Override
	public Condition visit(PropertyOperation op) {
		EntityMetadata entity = entitiesByAlias.get(op.getLeft().getEntityName());
		PropertyMetadata property = entity.getRequiredProperty(op.getLeft().getPropertyName());

		Expression left = toExpression(op.getLeft());
		
		switch (op.getOperator()) {
		case IS_NULL: return Conditions.isNull(left);
		case IS_NOT_NULL: return Conditions.isNull(left).not();
		}
		
		if (op.getValue() instanceof Collection) {
			Collection<?> value = (Collection<?>) op.getValue();
			List<Expression> expressions = new ArrayList<>(value.size());
			for (Object v : value)
				expressions.add(toExpression(v, property));
			
			switch (op.getOperator()) {
			case IN: return Conditions.in(left, expressions);
			case NOT_IN: return Conditions.in(left, expressions).not();
			default: throw new InvalidCriteriaException("Unexpected operator " + op.getOperator() + " on a collection");
			}
		}
		
		Object rightValue = op.getValue();
		if (property.isForeignKey() && rightValue != null && property.getType().isAssignableFrom(rightValue.getClass())) {
			// if foreign key, we need to use the id instead of the object
			EntityInstance<?> foreignKey = query.getClient().getInstance(rightValue);
			rightValue = foreignKey.getId();
		}
		Expression right = toExpression(rightValue, property);
		switch (op.getOperator()) {
		case EQUALS: return Conditions.isEqual(left, right);
		case NOT_EQUALS: return Conditions.isNotEqual(left, right);
		case GREATER_THAN: return Conditions.isGreater(left, right);
		case GREATER_THAN_OR_EQUAL: return Conditions.isGreaterOrEqualTo(left, right);
		case LESS_THAN: return Conditions.isLess(left, right);
		case LESS_THAN_OR_EQUAL: return Conditions.isLessOrEqualTo(left, right);
		case LIKE: return Conditions.like(left, right);
		case NOT_LIKE: return Conditions.like(left, right).not();
		case ARRAY_CONTAINS: return Conditions.isEqual(right, SimpleFunction.create("ANY", Collections.singletonList(left)));
		default: throw new InvalidCriteriaException("Unexpected operator " + op.getOperator());
		}
	}
	
	protected Expression toExpression(Object value, PropertyMetadata property) {
		if (value instanceof PropertyOperand)
			return toExpression((PropertyOperand)value);
		if (value != null && Enum.class.isAssignableFrom(value.getClass()))
			value = value.toString();
		return query.marker(query.getClient().getSchemaDialect().convertToDataBase(value, property));
	}
	
	protected Expression toExpression(PropertyOperand propertyOperand) {
		EntityMetadata rightEntity = entitiesByAlias.get(propertyOperand.getEntityName());
		Column column = Column.create(rightEntity.getRequiredProperty(propertyOperand.getPropertyName()).getColumnName(), tablesByAlias.get(propertyOperand.getEntityName()));
		Expression result = column;
		for (SqlFunction fct : propertyOperand.getFunctionsToApply())
			result = query.getClient().getSchemaDialect().applyFunctionTo(fct, result);
		return result;
	}

}
