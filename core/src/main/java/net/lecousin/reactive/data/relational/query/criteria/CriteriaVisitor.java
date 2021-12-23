package net.lecousin.reactive.data.relational.query.criteria;

import net.lecousin.reactive.data.relational.query.criteria.Criteria.And;
import net.lecousin.reactive.data.relational.query.criteria.Criteria.Or;

public interface CriteriaVisitor<T> {

	T visit(Criteria.And and);
	
	T visit(Criteria.Or or);
	
	T visit(Criteria.PropertyOperation op);
	
	public abstract static class SearchVisitor implements CriteriaVisitor<Boolean> {
		@Override
		public Boolean visit(And and) {
			return Boolean.valueOf(and.getLeft().accept(this).booleanValue() || and.getRight().accept(this).booleanValue());
		}
		
		@Override
		public Boolean visit(Or or) {
			return Boolean.valueOf(or.getLeft().accept(this).booleanValue() || or.getRight().accept(this).booleanValue());
		}
	}
	
}
