package net.lecousin.reactive.data.relational.query.criteria;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.data.r2dbc.dialect.BindMarker;
import org.springframework.data.r2dbc.dialect.BindMarkers;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.Table;

import net.lecousin.reactive.data.relational.query.criteria.Criteria.And;
import net.lecousin.reactive.data.relational.query.criteria.Criteria.Or;
import net.lecousin.reactive.data.relational.query.criteria.Criteria.PropertyOperand;
import net.lecousin.reactive.data.relational.query.criteria.Criteria.PropertyOperation;

public class CriteriaSqlBuilder implements CriteriaVisitor<Condition> {
	
	private Map<String, RelationalPersistentEntity<?>> entitiesByAlias;
	private Map<String, Table> tablesByAlias;
	private BindMarkers bindMarkers;
	private Map<BindMarker, Object> bindings;

	public CriteriaSqlBuilder(Map<String, RelationalPersistentEntity<?>> entitiesByAlias, Map<String, Table> tablesByAlias, BindMarkers bindMarkers, Map<BindMarker, Object> bindings) {
		this.entitiesByAlias = entitiesByAlias;
		this.tablesByAlias = tablesByAlias;
		this.bindMarkers = bindMarkers;
		this.bindings = bindings;
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
		Column left = Column.create(entity.getRequiredPersistentProperty(op.getLeft().getPropertyName()).getColumnName(), tablesByAlias.get(op.getLeft().getEntityName()));
		
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
		
		Expression right = toExpression(op.getValue());
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
		
		BindMarker marker = bindMarkers.next();
		bindings.put(marker, value);
		return SQL.bindMarker(marker.getPlaceholder());
	}

}
