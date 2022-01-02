package net.lecousin.reactive.data.relational.query.criteria;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.reactive.data.relational.schema.dialect.RelationalDatabaseSchemaDialect.SqlFunction;

/**
 * Criteria to use in where clause of a database query.
 */
public interface Criteria {
	
	/** Accept a visitor to go through all nodes of this criteria object. */
	<T> T accept(CriteriaVisitor<T> visitor);
	
	/** Create a AND condition with: (this criteria) AND (given criteria). */
	default Criteria and(Criteria criteria) {
		return new And(this, criteria);
	}
	
	/** Create a OR condition with: (this criteria) OR (given criteria). */
	default Criteria or(Criteria criteria) {
		return new Or(this, criteria);
	}
	
	/** Create a property operand to use in a property operation such as comparison. */
	static PropertyOperand property(String entityName, String propertyName) {
		return new PropertyOperand(entityName, propertyName);
	}

	/** And condition. */
	public static class And implements Criteria {
		private Criteria left;
		private Criteria right;
		
		public And(Criteria left, Criteria right) {
			this.left = left;
			this.right = right;
		}

		@Override
		public <T> T accept(CriteriaVisitor<T> visitor) {
			return visitor.visit(this);
		}
		
		public Criteria getLeft() {
			return left;
		}

		public Criteria getRight() {
			return right;
		}
		
		@Override
		public String toString() {
			return "(" + left.toString() + " AND " + right.toString() + ")";
		}
	}

	/** Or condition. */
	public static class Or implements Criteria {
		private Criteria left;
		private Criteria right;
		
		public Or(Criteria left, Criteria right) {
			this.left = left;
			this.right = right;
		}
		
		public Criteria getLeft() {
			return left;
		}

		public Criteria getRight() {
			return right;
		}

		@Override
		public <T> T accept(CriteriaVisitor<T> visitor) {
			return visitor.visit(this);
		}
		
		@Override
		public String toString() {
			return "(" + left.toString() + " OR " + right.toString() + ")";
		}
	}
	
	public enum PropertyOperator {
		EQUALS,
		NOT_EQUALS,
		GREATER_THAN,
		GREATER_THAN_OR_EQUAL,
		LESS_THAN,
		LESS_THAN_OR_EQUAL,
		IS_NULL,
		IS_NOT_NULL,
		LIKE,
		NOT_LIKE,
		IN,
		NOT_IN;
	}
	
	/** Property operand. */
	public static class PropertyOperand {
		private String entityName;
		private String propertyName;
		private LinkedList<SqlFunction> functionsToApply = new LinkedList<>();
		
		public PropertyOperand(String entityName, String propertyName) {
			this.entityName = entityName;
			this.propertyName = propertyName;
		}

		public String getEntityName() {
			return entityName;
		}

		public String getPropertyName() {
			return propertyName;
		}
		
		public List<SqlFunction> getFunctionsToApply() {
			return new ArrayList<>(functionsToApply);
		}
		
		/** Create a 'is equal' condition on this property with the given value.
		 * The value may be an entity in case this property is a foreign key, another property operand for properties comparison, or a literal value such as a number, string, date...
		 */
		public Criteria is(Object value) {
			return new PropertyOperation(this, PropertyOperator.EQUALS, value);
		}
		
		/** Equivalent to is(Criteria.property(entityName, propertyName)). */
		public Criteria is(String entityName, String propertyName) {
			return new PropertyOperation(this, PropertyOperator.EQUALS, new PropertyOperand(entityName, propertyName));
		}
		
		/** Create a 'is not equal' condition on this property with the given value.
		 * The value may be an entity in case this property is a foreign key, another property operand for properties comparison, or a literal value such as a number, string, date...
		 */
		public Criteria isNot(Object value) {
			return new PropertyOperation(this, PropertyOperator.NOT_EQUALS, value);
		}
		
		/** Equivalent to isNot(Criteria.property(entityName, propertyName)). */
		public Criteria isNot(String entityName, String propertyName) {
			return new PropertyOperation(this, PropertyOperator.NOT_EQUALS, new PropertyOperand(entityName, propertyName));
		}
		
		/** Create a '&gt;' condition on this property with the given value.
		 * The property must be comparable with the given value, which may be a property operand for a comparison between two properties.
		 */
		public Criteria greaterThan(Object value) {
			return new PropertyOperation(this, PropertyOperator.GREATER_THAN, value);
		}
		
		/** Equivalent to greaterThan(Criteria.property(entityName, propertyName)). */
		public Criteria greaterThan(String entityName, String propertyName) {
			return new PropertyOperation(this, PropertyOperator.GREATER_THAN, new PropertyOperand(entityName, propertyName));
		}
		
		/** Create a '&gt;=' condition on this property with the given value.
		 * The property must be comparable with the given value, which may be a property operand for a comparison between two properties.
		 */
		public Criteria greaterOrEqualTo(Object value) {
			return new PropertyOperation(this, PropertyOperator.GREATER_THAN_OR_EQUAL, value);
		}
		
		/** Equivalent to greaterOrEqualTo(Criteria.property(entityName, propertyName)). */
		public Criteria greaterOrEqualTo(String entityName, String propertyName) {
			return new PropertyOperation(this, PropertyOperator.GREATER_THAN_OR_EQUAL, new PropertyOperand(entityName, propertyName));
		}
		
		/** Create a '&lt;' condition on this property with the given value.
		 * The property must be comparable with the given value, which may be a property operand for a comparison between two properties.
		 */
		public Criteria lessThan(Object value) {
			return new PropertyOperation(this, PropertyOperator.LESS_THAN, value);
		}
		
		/** Equivalent to lessThan(Criteria.property(entityName, propertyName)). */
		public Criteria lessThan(String entityName, String propertyName) {
			return new PropertyOperation(this, PropertyOperator.LESS_THAN, new PropertyOperand(entityName, propertyName));
		}
		
		/** Create a '&lt;=' condition on this property with the given value.
		 * The property must be comparable with the given value, which may be a property operand for a comparison between two properties.
		 */
		public Criteria lessOrEqualTo(Object value) {
			return new PropertyOperation(this, PropertyOperator.LESS_THAN_OR_EQUAL, value);
		}
		
		/** Equivalent to lessOrEqualTo(Criteria.property(entityName, propertyName)). */
		public Criteria lessOrEqualTo(String entityName, String propertyName) {
			return new PropertyOperation(this, PropertyOperator.LESS_THAN_OR_EQUAL, new PropertyOperand(entityName, propertyName));
		}
		
		/** Create a IS NULL condition on this property. */
		public Criteria isNull() {
			return new PropertyOperation(this, PropertyOperator.IS_NULL, null);
		}
		
		/** Create a IS NOT NULL condition on this property. */
		public Criteria isNotNull() {
			return new PropertyOperation(this, PropertyOperator.IS_NOT_NULL, null);
		}
		
		/** Create a LIKE condition on this property. */
		public Criteria like(Object value) {
			return new PropertyOperation(this, PropertyOperator.LIKE, value);
		}
		
		/** Equivalent to like(Criteria.property(entityName, propertyName)). */
		public Criteria like(String entityName, String propertyName) {
			return new PropertyOperation(this, PropertyOperator.LIKE, new PropertyOperand(entityName, propertyName));
		}
		
		/** Create a NOT LIKE condition on this property. */
		public Criteria notLike(Object value) {
			return new PropertyOperation(this, PropertyOperator.NOT_LIKE, value);
		}
		
		/** Equivalent to notLike(Criteria.property(entityName, propertyName)). */
		public Criteria notLike(String entityName, String propertyName) {
			return new PropertyOperation(this, PropertyOperator.NOT_LIKE, new PropertyOperand(entityName, propertyName));
		}
		
		/** Create a IN condition to compare this property with the set of given values. */
		public Criteria in(Collection<?> values) {
			return new PropertyOperation(this, PropertyOperator.IN, values);
		}
		
		/** Create a NOT IN condition to compare this property with the set of given values. */
		public Criteria notIn(Collection<?> values) {
			return new PropertyOperation(this, PropertyOperator.NOT_IN, values);
		}
		
		/** Apply the UPPER function to this property to use in a comparison operation. */
		public PropertyOperand toUpperCase() {
			functionsToApply.addFirst(SqlFunction.UPPER);
			return this;
		}
		
		/** Apply the LOWER function to this property to use in a comparison operation. */
		public PropertyOperand toLowerCase() {
			functionsToApply.addFirst(SqlFunction.LOWER);
			return this;
		}
		
		/** Extract the ISO day of week from this property that must have a compatible type for this operation. */
		public PropertyOperand dateToIsoDayOfWeek() {
			functionsToApply.addFirst(SqlFunction.ISO_DAY_OF_WEEK);
			return this;
		}
		
		/** Extract the day of month from this property that must have a compatible type for this operation. */
		public PropertyOperand dateToDayOfMonth() {
			functionsToApply.addFirst(SqlFunction.DAY_OF_MONTH);
			return this;
		}
		
		/** Extract the day of year from this property that must have a compatible type for this operation. */
		public PropertyOperand dateToDayOfYear() {
			functionsToApply.addFirst(SqlFunction.DAY_OF_YEAR);
			return this;
		}
		
		/** Extract the month from this property that must have a compatible type for this operation. */
		public PropertyOperand dateToMonth() {
			functionsToApply.addFirst(SqlFunction.MONTH);
			return this;
		}
		
		/** Extract the year from this property that must have a compatible type for this operation. */
		public PropertyOperand dateToYear() {
			functionsToApply.addFirst(SqlFunction.YEAR);
			return this;
		}
		
		/** Extract the ISO week number from this property that must have a compatible type for this operation. */
		public PropertyOperand dateToIsoWeek() {
			functionsToApply.addFirst(SqlFunction.ISO_WEEK);
			return this;
		}

		/** Extract the hour from this property that must have a compatible type for this operation. */
		public PropertyOperand timeToHour() {
			functionsToApply.addFirst(SqlFunction.HOUR);
			return this;
		}

		/** Extract the minute from this property that must have a compatible type for this operation. */
		public PropertyOperand timeToMinute() {
			functionsToApply.addFirst(SqlFunction.MINUTE);
			return this;
		}

		/** Extract the second from this property that must have a compatible type for this operation. */
		public PropertyOperand timeToSecond() {
			functionsToApply.addFirst(SqlFunction.SECOND);
			return this;
		}
		
		@Override
		public String toString() {
			return entityName + "." + propertyName;
		}
	}

	public static class PropertyOperation implements Criteria {
		private PropertyOperand left;
		private PropertyOperator operator;
		private Object value;
		
		public PropertyOperation(PropertyOperand left, PropertyOperator operator, Object value) {
			this.left = left;
			this.operator = operator;
			this.value = value;
		}

		public PropertyOperand getLeft() {
			return left;
		}

		public PropertyOperator getOperator() {
			return operator;
		}

		public Object getValue() {
			return value;
		}

		@Override
		public <T> T accept(CriteriaVisitor<T> visitor) {
			return visitor.visit(this);
		}
		
		@Override
		public String toString() {
			return left + " " + operator + " " + value;
		}
	}
	
}
