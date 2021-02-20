package net.lecousin.reactive.data.relational.model;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.lecousin.reactive.data.relational.enhance.Enhancer;

@SuppressWarnings({"java:S3011"})
public class JoinTableCollectionFromTargetCollection<J, T> implements Collection<J> {

	private Object sourceInstance;
	private Class<J> joinClass;
	private Set<T> targetCollection;
	private Field sourceField;
	private Field targetField;
	private Collection<J> originalCollection;

	@SuppressWarnings("unchecked")
	public JoinTableCollectionFromTargetCollection(Object sourceInstance, Collection<J> originalCollection, Set<T> targetCollection, String joinClassName, int sourceAttributeLinkNumber) {
		this.sourceInstance = sourceInstance;
		this.targetCollection = targetCollection;
		this.originalCollection = originalCollection != null ? new ArrayList<>(originalCollection) : new ArrayList<>(0);
		try {
			joinClass = (Class<J>) getClass().getClassLoader().loadClass(joinClassName);
			sourceField = joinClass.getDeclaredField(Enhancer.JOIN_TABLE_ATTRIBUTE_PREFIX + sourceAttributeLinkNumber);
			sourceField.setAccessible(true);
			targetField = joinClass.getDeclaredField(Enhancer.JOIN_TABLE_ATTRIBUTE_PREFIX + (sourceAttributeLinkNumber == 1 ? 2 : 1));
			targetField.setAccessible(true);
		} catch (Exception e) {
			throw new ModelAccessException("Error initializing JoinTableCollectionFromTargetCollection on " + joinClassName, e);
		}
	}
	
	@SuppressWarnings({"unchecked", "java:S3776"})
	private J getOriginalOrCreate(T target) {
		for (J join : originalCollection)
			try {
				if (Objects.equals(targetField.get(join), target))
					return join;
			} catch (Exception e) {
				// ignore
			}
		Collection<J> col = LcEntityTypeInfo.get(target.getClass()).getJoinTableElementsForJoinTableClass(target, joinClass);
		if (col != null) {
			if (col instanceof JoinTableCollectionFromTargetCollection) {
				col = ((JoinTableCollectionFromTargetCollection<J, ?>)col).originalCollection;
			}
			for (J join : col)
				try {
					if (Objects.equals(sourceField.get(join), sourceInstance)) {
						originalCollection.add(join);
						return join;
					}
				} catch (Exception e) {
					// ignore
				}
		}
		
		try {
			J join = joinClass.getConstructor().newInstance();
			sourceField.set(join, sourceInstance);
			targetField.set(join, target);
			originalCollection.add(join);
			return join;
		} catch (Exception e) {
			throw new ModelAccessException("Error creating join entity " + joinClass.getName(), e);
		}
	}

	@Override
	public int size() {
		return targetCollection.size();
	}

	@Override
	public boolean isEmpty() {
		return targetCollection.isEmpty();
	}

	@Override
	public void clear() {
		targetCollection.clear();
	}

	@Override
	public boolean contains(Object o) {
		if (o == null || !joinClass.equals(o.getClass()))
			return false;
		try {
			if (sourceField.get(o) != sourceInstance)
				return false;
			return targetCollection.contains(targetField.get(o));
		} catch (Exception e) {
			throw new ModelAccessException("Error accessing field from " + o);
		}
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c)
			if (!contains(o))
				return false;
		return true;
	}

	@Override
	public Iterator<J> iterator() {
		Iterator<T> it = targetCollection.iterator();
		return new Iterator<J>() {
			@Override
			public boolean hasNext() {
				return it.hasNext();
			}
			
			@Override
			public J next() {
				return getOriginalOrCreate(it.next());
			}
		};
	}

	@Override
	public Object[] toArray() {
		return toArray(new Object[size()]);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R[] toArray(R[] a) {
        if (a.length < targetCollection.size())
            a = (R[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), targetCollection.size());
        int i = 0;
        for (J e : this)
        	a[i++] = (R) e;
        return a;
	}

	@Override
	public boolean add(J e) {
		try {
			@SuppressWarnings("unchecked")
			T target = (T) targetField.get(e);
			if (targetCollection.contains(target))
				return false;
			originalCollection.add(e);
			targetCollection.add(target);
			return true;
		} catch (Exception err) {
			throw new ModelAccessException("Error adding join " + joinClass.getName(), err);
		}
	}

	@Override
	public boolean addAll(Collection<? extends J> c) {
		boolean result = false;
		for (J e : c)
			result |= add(e);
		return result;
	}

	@Override
	public boolean remove(Object o) {
		try {
			return targetCollection.remove(targetField.get(o));
		} catch (Exception e) {
			throw new ModelAccessException("Error removing join " + joinClass.getName(), e);
		}
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean result = false;
		for (Object e : c)
			result |= remove(e);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean retainAll(Collection<?> c) {
		List<T> list = new ArrayList<>(c.size());
		for (Object o : c)
			try {
				list.add((T) targetField.get(o));
			} catch (Exception e) {
				throw new ModelAccessException("Error getting " + targetField.getName() + " from " + o, e);
			}
		return targetCollection.retainAll(list);
	}

}
