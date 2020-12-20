package net.lecousin.reactive.data.relational.query.criteria;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Table;

import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.query.SqlQuery;
import net.lecousin.reactive.data.relational.query.criteria.Criteria.And;
import net.lecousin.reactive.data.relational.query.criteria.Criteria.Or;
import net.lecousin.reactive.data.relational.query.criteria.Criteria.PropertyOperand;
import net.lecousin.reactive.data.relational.query.criteria.Criteria.PropertyOperation;

public class CriteriaSqlBuilder implements CriteriaVisitor<Condition> {
	
	private Map<String, RelationalPersistentEntity<?>> entitiesByAlias;
	private Map<String, Table> tablesByAlias;
	private SqlQuery<?> query;

	public CriteriaSqlBuilder(Map<String, RelationalPersistentEntity<?>> entitiesByAlias, Map<String, Table> tablesByAlias, SqlQuery<?> query) {
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
		RelationalPersistentEntity<?> entity = entitiesByAlias.get(op.getLeft().getEntityName());
		RelationalPersistentProperty property = entity.getRequiredPersistentProperty(op.getLeft().getPropertyName());
		Column left = Column.create(property.getColumnName(), tablesByAlias.get(op.getLeft().getEntityName()));
		
		switch (op.getOperator()) {
		case IS_NULL: return Conditions.isNull(left);
		case IS_NOT_NULL: return Conditions.isNull(left).not();
		}
		
		if (op.getValue() instanceof Collection) {
			Collection<?> value = (Collection<?>) op.getValue();
			List<Expression> expressions = new ArrayList<>(value.size());
			for (Object v : value)
				expressions.add(toExpression(v));
			
			switch (op.getOperator()) {
			case IN: return Conditions.in(left, expressions);
			case NOT_IN: return Conditions.in(left, expressions).not();
			default: throw new InvalidCriteriaException("Unexpected operator " + op.getOperator() + " on a collection");
			}
		}
		
		Object rightValue = op.getValue();
		if (property.isAnnotationPresent(ForeignKey.class) && rightValue != null && property.getType().isAssignableFrom(rightValue.getClass())) {
			// if foreign key, we need to use the id instead of the object
			MappingContext<RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> context = query.getClient().getMappingContext();
			RelationalPersistentEntity<?> foreignEntity = context.getRequiredPersistentEntity(property.getType());
			rightValue = ModelUtils.getId(foreignEntity, foreignEntity.getPropertyAccessor(rightValue), context);
		}
		Expression right = toExpression(rightValue);
		switch (op.getOperator()) {
		case EQUALS: return Conditions.isEqual(left, right);
		case NOT_EQUALS: return Conditions.isNotEqual(left, right);
		case GREATER_THAN: return Conditions.isGreater(left, right);
		case GREATER_THAN_OR_EQUAL: return Conditions.isGreaterOrEqualTo(left, right);
		case LESS_THAN: return Conditions.isLess(left, right);
		case LESS_THAN_OR_EQUAL: return Conditions.isLessOrEqualTo(left, right);
		case LIKE: return Conditions.like(left, right);
		case NOT_LIKE: return Conditions.like(left, right).not();
		default: throw new InvalidCriteriaException("Unexpected operator " + op.getOperator());
		}
	}
	
	private Expression toExpression(Object value) {
		if (value instanceof PropertyOperand) {
			PropertyOperand p = (PropertyOperand)value;
			RelationalPersistentEntity<?> rightEntity = entitiesByAlias.get(p.getEntityName());
			return Column.create(rightEntity.getRequiredPersistentProperty(p.getPropertyName()).getColumnName(), tablesByAlias.get(p.getEntityName()));
		}
		return query.marker(value);
	}

}
