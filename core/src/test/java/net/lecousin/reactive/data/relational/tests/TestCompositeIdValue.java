package net.lecousin.reactive.data.relational.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.lecousin.reactive.data.relational.model.CompositeIdValue;

class TestCompositeIdValue {

	@Test
	void test() {
		CompositeIdValue c = new CompositeIdValue();
		CompositeIdValue c2 = new CompositeIdValue();
		Assertions.assertTrue(c.isNull());
		Assertions.assertFalse(c.equals(null));
		Assertions.assertTrue(c.equals(c2));
		Assertions.assertTrue(c2.equals(c));
		Assertions.assertEquals(c.hashCode(), c2.hashCode());
		c2.add("test", 1);
		Assertions.assertFalse(c.equals(c2));
		Assertions.assertFalse(c2.equals(c));
		Assertions.assertFalse(c2.isNull());
		c.add("test", 1);
		Assertions.assertTrue(c.equals(c2));
		Assertions.assertTrue(c2.equals(c));
		Assertions.assertEquals(c.hashCode(), c2.hashCode());
		Assertions.assertFalse(c.isNull());
		c2.add("test2", null);
		Assertions.assertFalse(c2.isNull());
		Assertions.assertFalse(c.equals(c2));
		Assertions.assertFalse(c2.equals(c));
		c2.hashCode();
		c.add("test2", 2);
		Assertions.assertFalse(c.equals(c2));
		Assertions.assertFalse(c2.equals(c));
		c2.add("test2", 2);
		Assertions.assertTrue(c.equals(c2));
		Assertions.assertTrue(c2.equals(c));
		c.add("test3", 3);
		c2.add("test4", 4);
		Assertions.assertFalse(c.equals(c2));
		Assertions.assertFalse(c2.equals(c));
	}

}
