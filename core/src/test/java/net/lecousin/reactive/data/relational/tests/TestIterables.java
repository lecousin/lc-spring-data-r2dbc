package net.lecousin.reactive.data.relational.tests;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.lecousin.reactive.data.relational.util.Iterables;

class TestIterables {

	@Test
	void testFilterIterable() {
		Iterable<Integer> iterable = Arrays.asList(10, 11, 12, 13, 14, 15);
		Iterable<Integer> filtered = Iterables.filter(iterable, i -> (i % 2) == 0);
		Iterator<Integer> it = filtered.iterator();
		Assertions.assertTrue(it.hasNext());
		Assertions.assertEquals(10, it.next());
		Assertions.assertTrue(it.hasNext());
		Assertions.assertEquals(12, it.next());
		Assertions.assertTrue(it.hasNext());
		Assertions.assertEquals(14, it.next());
		Assertions.assertFalse(it.hasNext());
		Assertions.assertThrows(NoSuchElementException.class, () -> it.next());
	}
	
	@Test
	void testMapIterable() {
		Iterable<Integer> iterable = Arrays.asList(10, 11, 12, 13, 14, 15);
		Iterable<String> mapped = Iterables.map(iterable, i -> i.toString());
		Iterator<String> it = mapped.iterator();
		Assertions.assertTrue(it.hasNext());
		Assertions.assertEquals("10", it.next());
		Assertions.assertTrue(it.hasNext());
		Assertions.assertEquals("11", it.next());
		Assertions.assertTrue(it.hasNext());
		Assertions.assertEquals("12", it.next());
		Assertions.assertTrue(it.hasNext());
		Assertions.assertEquals("13", it.next());
		Assertions.assertTrue(it.hasNext());
		Assertions.assertEquals("14", it.next());
		Assertions.assertTrue(it.hasNext());
		Assertions.assertEquals("15", it.next());
		Assertions.assertFalse(it.hasNext());
		Assertions.assertThrows(NoSuchElementException.class, () -> it.next());
	}

}
