package net.lecousin.reactive.data.relational.query.criteria;

import net.lecousin.reactive.data.relational.query.criteria.Criteria.And;
import net.lecousin.reactive.data.relational.query.criteria.Criteria.PropertyOperation;
import net.lecousin.reactive.data.relational.query.criteria.Criteria.Or;

public interface CriteriaVisitor<T> {

	T visit(Criteria.And and);
	
	T visit(Criteria.Or or);
	
	T visit(Criteria.PropertyOperation op);
	
	public static class DefaultVisitor<T> implements CriteriaVisitor<T> {
		@Override
		public T visit(And and) {
			and.getLeft().accept(this);
			return and.getRight().accept(this);
		}
		
		@Override
		public T visit(Or or) {
			or.getLeft().accept(this);
			return or.getRight().accept(this);
		}
		
		@Override
		public T visit(PropertyOperation op) {
			// nothing
			return null;
		}
	}
	
	public abstract static class SearchVisitor implements CriteriaVisitor<Boolean> {
		@Override
		public Boolean visit(And and) {
			return Boolean.valueOf(and.getLeft().accept(this).booleanValue() && and.getRight().accept(this).booleanValue());
		}
		
		@Override
		public Boolean visit(Or or) {
			return Boolean.valueOf(or.getLeft().accept(this).booleanValue() && or.getRight().accept(this).booleanValue());
		}
	}
	
}
