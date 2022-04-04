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
package net.lecousin.reactive.data.relational.model;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Utility methods to convert primitive arrays.
 * 
 * @author Guillaume Le Cousin
 *
 */
@SuppressWarnings({"java:S1192", "java:S3776"})
public class PrimitiveArraysUtil {

	private PrimitiveArraysUtil() {
		// no instance
	}
	
	public static Object primitiveArrayToObjectArray(Object primitiveArray) {
		Class<?> primitiveType = primitiveArray.getClass().getComponentType();
		if (boolean.class.equals(primitiveType)) {
			boolean[] values = (boolean[])primitiveArray;
			Boolean[] nonPrimitive = new Boolean[values.length];
			for (int i = 0; i < values.length; ++i)
				nonPrimitive[i] = Boolean.valueOf(values[i]);
			return nonPrimitive;
		}
		if (short.class.equals(primitiveType)) {
			short[] values = (short[])primitiveArray;
			Short[] nonPrimitive = new Short[values.length];
			for (int i = 0; i < values.length; ++i)
				nonPrimitive[i] = Short.valueOf(values[i]);
			return nonPrimitive;
		}
		if (long.class.equals(primitiveType)) {
			long[] values = (long[])primitiveArray;
			Long[] nonPrimitive = new Long[values.length];
			for (int i = 0; i < values.length; ++i)
				nonPrimitive[i] = Long.valueOf(values[i]);
			return nonPrimitive;
		}
		if (int.class.equals(primitiveType)) {
			int[] values = (int[])primitiveArray;
			Integer[] nonPrimitive = new Integer[values.length];
			for (int i = 0; i < values.length; ++i)
				nonPrimitive[i] = Integer.valueOf(values[i]);
			return nonPrimitive;
		}
		if (float.class.equals(primitiveType)) {
			float[] values = (float[])primitiveArray;
			Float[] nonPrimitive = new Float[values.length];
			for (int i = 0; i < values.length; ++i)
				nonPrimitive[i] = Float.valueOf(values[i]);
			return nonPrimitive;
		}
		if (double.class.equals(primitiveType)) {
			double[] values = (double[])primitiveArray;
			Double[] nonPrimitive = new Double[values.length];
			for (int i = 0; i < values.length; ++i)
				nonPrimitive[i] = Double.valueOf(values[i]);
			return nonPrimitive;
		}
		throw new ModelAccessException("Primitive array not supported: " + primitiveArray.getClass());
	}
	
	@SuppressWarnings({"java:S1452"})
	public static Collection<?> primitiveArrayToCollection(Object primitiveArray) {
		Class<?> primitiveType = primitiveArray.getClass().getComponentType();
		if (boolean.class.equals(primitiveType)) {
			boolean[] primitives = (boolean[]) primitiveArray;
			ArrayList<Boolean> list = new ArrayList<>(primitives.length);
			for (int i = 0; i < primitives.length; ++i)
				list.add(Boolean.valueOf(primitives[i]));
			return list;
		}
		if (short.class.equals(primitiveType)) {
			short[] primitives = (short[]) primitiveArray;
			ArrayList<Short> list = new ArrayList<>(primitives.length);
			for (int i = 0; i < primitives.length; ++i)
				list.add(Short.valueOf(primitives[i]));
			return list;
		}
		if (int.class.equals(primitiveType)) {
			int[] primitives = (int[]) primitiveArray;
			ArrayList<Integer> list = new ArrayList<>(primitives.length);
			for (int i = 0; i < primitives.length; ++i)
				list.add(Integer.valueOf(primitives[i]));
			return list;
		}
		if (long.class.equals(primitiveType)) {
			long[] primitives = (long[]) primitiveArray;
			ArrayList<Long> list = new ArrayList<>(primitives.length);
			for (int i = 0; i < primitives.length; ++i)
				list.add(Long.valueOf(primitives[i]));
			return list;
		}
		if (float.class.equals(primitiveType)) {
			float[] primitives = (float[]) primitiveArray;
			ArrayList<Float> list = new ArrayList<>(primitives.length);
			for (int i = 0; i < primitives.length; ++i)
				list.add(Float.valueOf(primitives[i]));
			return list;
		}
		if (double.class.equals(primitiveType)) {
			double[] primitives = (double[]) primitiveArray;
			ArrayList<Double> list = new ArrayList<>(primitives.length);
			for (int i = 0; i < primitives.length; ++i)
				list.add(Double.valueOf(primitives[i]));
			return list;
		}
		throw new ModelAccessException("Primitive array not supported: " + primitiveArray.getClass());
	}
	
	public static Object objectArrayToPrimitiveArray(Object array, Class<?> primitiveType) {
		if (boolean.class.equals(primitiveType)) {
			Boolean[] a = (Boolean[])array;
			boolean[] b = new boolean[a.length];
			for (int i = 0; i < a.length; ++i)
				b[i] = a[i].booleanValue();
			return b;
		}
		if (short.class.equals(primitiveType)) {
			Short[] a = (Short[])array;
			short[] b = new short[a.length];
			for (int i = 0; i < a.length; ++i)
				b[i] = a[i].shortValue();
			return b;
		}
		if (int.class.equals(primitiveType)) {
			Integer[] a = (Integer[])array;
			int[] b = new int[a.length];
			for (int i = 0; i < a.length; ++i)
				b[i] = a[i].intValue();
			return b;
		}
		if (long.class.equals(primitiveType)) {
			Long[] a = (Long[])array;
			long[] b = new long[a.length];
			for (int i = 0; i < a.length; ++i)
				b[i] = a[i].longValue();
			return b;
		}
		if (float.class.equals(primitiveType)) {
			Float[] a = (Float[])array;
			float[] b = new float[a.length];
			for (int i = 0; i < a.length; ++i)
				b[i] = a[i].floatValue();
			return b;
		}
		if (double.class.equals(primitiveType)) {
			Double[] a = (Double[])array;
			double[] b = new double[a.length];
			for (int i = 0; i < a.length; ++i)
				b[i] = a[i].doubleValue();
			return b;
		}
		throw new ModelAccessException("Primitive array not supported: " + primitiveType);
	}

}
