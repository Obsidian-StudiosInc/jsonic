/*
 * Copyright 2014 Hidekatsu Izuno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.arnx.jsonic.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.io.ObjectStreamClass;

public final class ClassUtil {
	private static final Map<ClassLoader, Map<String, Class<?>>> cache =
		new WeakHashMap<ClassLoader, Map<String, Class<?>>>();

	public static Class<?> findClass(String name) {
		ClassLoader cl;
		try {
			cl = Thread.currentThread().getContextClassLoader();
		} catch (SecurityException e) {
			cl = null;
		}

		Map<String, Class<?>> map;
		synchronized (cache) {
			map = cache.get(cl);

			if (map == null) {
				map = new LinkedHashMap<String, Class<?>>(16, 0.75f, true) {
					protected boolean removeEldestEntry(Map.Entry<String, Class<?>> eldest) {
						return size() > 1024;
					};
				};
				cache.put(cl, map);
			}
		}
		synchronized (map) {
			if (!map.containsKey(name)) {
				Class<?> target;
				try {
					if (cl != null) {
						target = cl.loadClass(name);
					} else {
						target = Class.forName(name);
					}
				} catch (ClassNotFoundException e) {
					target = null;
				}
				map.put(name, target);
			}
			return map.get(name);
		}
	}

	public static void clear() {
		synchronized (cache) {
			cache.clear();
		}
	}

	public static String toUpperCamel(String name) {
		StringBuilder sb = new StringBuilder(name.length());
		boolean toUpperCase = true;
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (c == ' ' || c == '_' || c == '-') {
				toUpperCase = true;
			} else if (toUpperCase) {
				sb.append(Character.toUpperCase(c));
				toUpperCase = false;
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static String toLowerCamel(String name) {
		StringBuilder sb = new StringBuilder(name.length());
		boolean toUpperCase = false;
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (c == ' ' || c == '_' || c == '-') {
				toUpperCase = true;
			} else if (toUpperCase) {
				sb.append(Character.toUpperCase(c));
				toUpperCase = false;
			} else {
				sb.append(c);
			}
		}
		if (sb.length() > 1 && Character.isUpperCase(sb.charAt(0)) && !Character.isUpperCase(sb.charAt(1))) {
			sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
		}
		return sb.toString();
	}

	public static Class<?> getRawType(Type t) {
		if (t instanceof Class<?>) {
			return (Class<?>)t;
		} else if (t instanceof ParameterizedType) {
			return (Class<?>)((ParameterizedType)t).getRawType();
		} else if (t instanceof GenericArrayType) {
			Class<?> cls = null;
			try {
				cls = Array.newInstance(getRawType(((GenericArrayType)t).getGenericComponentType()), 0).getClass();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return cls;
		} else if (t instanceof WildcardType) {
			Type[] types = ((WildcardType)t).getUpperBounds();
			return (types.length > 0) ? getRawType(types[0]) : Object.class;
		} else {
			return Object.class;
		}
	}

	public static Type getResolvedType(Type ptype, Class<?> pcls, Type type) {
		Map<Type, Type> map = new HashMap<Type, Type>();
		collectTypeVariableMap(ptype, pcls, map);

		Type result;
		if (type instanceof ParameterizedType) {
			ParameterizedType paramType = (ParameterizedType)type;
			Type[] paramTypeArgs = paramType.getActualTypeArguments();
			for (int i = 0; i < paramTypeArgs.length; i++) {
				Type iresult = paramTypeArgs[i];
				while (iresult instanceof TypeVariable<?>) {
					Type next = map.get(iresult);
					if (next != null) {
						iresult = next;
					} else {
						break;
					}
				}
				paramTypeArgs[i] = iresult;
			}
			result = new ParameterizedTypeImpl(paramType, paramTypeArgs);
		} else {
			result = type;
			while (result instanceof TypeVariable<?>) {
				Type next = map.get(result);
				if (next != null) {
					result = next;
				} else {
					break;
				}
			}
		}

		return result;
	}

	private static void collectTypeVariableMap(Type t, Class<?> c, Map<Type, Type> map) {
		if (t instanceof ParameterizedType) {
			TypeVariable<?>[] vs = c.getTypeParameters();
			Type[] as = ((ParameterizedType)t).getActualTypeArguments();
			if (vs != null && as != null && vs.length == as.length) {
				for (int i = 0; i < vs.length; i++) {
					map.put(vs[i], as[i]);
				}
			}

			Type ot = ((ParameterizedType)t).getOwnerType();
			if (ot instanceof ParameterizedType) {
				vs = ClassUtil.getRawType(ot).getTypeParameters();
				as = ((ParameterizedType)ot).getActualTypeArguments();
				if (vs != null && as != null && vs.length == as.length) {
					for (int i = 0; i < vs.length; i++) {
						map.put(vs[i], as[i]);
					}
				}
			}
		}

		Type[] its = c.getGenericInterfaces();
		if (its != null) {
			for (Type it : its) {
				Class<?> ic = ClassUtil.getRawType(it);
				collectTypeVariableMap(it, ic, map);
			}
		}

		Type st = c.getGenericSuperclass();
		while (st != null && st != Object.class) {
			Class<?> sc = ClassUtil.getRawType(st);
			collectTypeVariableMap(st, sc, map);
			st = sc.getGenericSuperclass();
		}
	}

	public static byte[] serialize(Object o) throws ObjectStreamException {
		ByteArrayOutputStream array = new ByteArrayOutputStream();
		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(array);
			out.writeObject(o);
			out.close();
		} catch (ObjectStreamException e) {
			throw e;
		} catch (IOException e) {
			// no handle
		}
		return array.toByteArray();
	}

	public static Object deserialize(byte[] data) throws ObjectStreamException, ClassNotFoundException {
		Object ret = null;
		ObjectInputStream in = null;
		try {
			in = new ContextObjectInputStream(new ByteArrayInputStream(data));
			ret = in.readObject();
			in.close();
		} catch (ObjectStreamException e) {
			throw e;
		} catch (IOException e) {
			// no handle
		}
		return ret;
	}

	public static int hashCode(Object target) {
		if (target == null) return 0;
		final int prime = 31;
		int result = 1;

		Class<?> current = target.getClass();
		do {
			for (Field f : current.getDeclaredFields()) {
				if (Modifier.isStatic(f.getModifiers())
						|| Modifier.isTransient(f.getModifiers())
						|| f.isSynthetic()) {
					continue;
				}

				Object self;
				try {
					f.setAccessible(true);
					self = f.get(target);
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
				if (self == null) {
					result = prime * result + 0;
				} else if (self.getClass().isArray()) {
					if (self.getClass() == boolean[].class) {
						result = prime * result + Arrays.hashCode((boolean[])self);
					} else if (self.getClass() == char[].class) {
						result = prime * result + Arrays.hashCode((char[])self);
					} else if (self.getClass() == byte[].class) {
						result = prime * result + Arrays.hashCode((byte[])self);
					} else if (self.getClass() == short[].class) {
						result = prime * result + Arrays.hashCode((short[])self);
					} else if (self.getClass() == int[].class) {
						result = prime * result + Arrays.hashCode((int[])self);
					} else if (self.getClass() == long[].class) {
						result = prime * result + Arrays.hashCode((long[])self);
					} else if (self.getClass() == float[].class) {
						result = prime * result + Arrays.hashCode((float[])self);
					} else if (self.getClass() == double[].class) {
						result = prime * result + Arrays.hashCode((double[])self);
					} else {
						result = prime * result + Arrays.hashCode((Object[])self);
					}
				} else {
					result = prime * result + self.hashCode();
				}

				System.out.println(f.getName() + ": " + result);
			}
			current = current.getSuperclass();
		} while (current != Object.class);

		return result;
	}

	public static boolean equals(Object target, Object o) {
		if (target == o) return true;
		if (target == null || o == null) return false;
		if (target.getClass() != o.getClass()) return false;

		Class<?> current = target.getClass();
		do {
			for (Field f : current.getDeclaredFields()) {
				if (Modifier.isStatic(f.getModifiers())
						|| Modifier.isTransient(f.getModifiers())
						|| f.isSynthetic()) {
					continue;
				}

				Object self;
				Object other;
				try {
					f.setAccessible(true);
					self = f.get(target);
					other = f.get(o);
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
				if (self == null) {
					if (other != null) return false;
				} else if (self.getClass().isArray()) {
					if (self.getClass() == boolean[].class) {
						if (!Arrays.equals((boolean[])self, (boolean[])other)) return false;
					} else if (self.getClass() == char[].class) {
						if (!Arrays.equals((char[])self, (char[])other)) return false;
					} else if (self.getClass() == byte[].class) {
						if (!Arrays.equals((byte[])self, (byte[])other)) return false;
					} else if (self.getClass() == short[].class) {
						if (!Arrays.equals((short[])self, (short[])other)) return false;
					} else if (self.getClass() == int[].class) {
						if (!Arrays.equals((int[])self, (int[])other)) return false;
					} else if (self.getClass() == long[].class) {
						if (!Arrays.equals((long[])self, (long[])other)) return false;
					} else if (self.getClass() == float[].class) {
						if (!Arrays.equals((float[])self, (float[])other)) return false;
					} else if (self.getClass() == double[].class) {
						if (!Arrays.equals((double[])self, (double[])other)) return false;
					} else {
						if (!Arrays.equals((Object[])self, (Object[])other)) return false;
					}
				} else if (!self.equals(other)) {
					return false;
				}
			}
			current = current.getSuperclass();
		} while (current != Object.class);

		return true;
	}

	public static String toString(Object target) {
		if (target == null) return "null";

		BeanInfo info = BeanInfo.get(target.getClass());

		StringBuilder sb = new StringBuilder(10 * info.getProperties().size() + 20);
		sb.append(target.getClass().getSimpleName()).append(" [");
		boolean first = true;
		for (PropertyInfo prop : info.getProperties()) {
			if (!prop.isReadable() || prop.getName().equals("class")) continue;

			if (!first) {
				sb.append(", ");
			} else {
				first = false;
			}
			sb.append(prop.getName()).append("=");
			try {
				Object value = prop.get(target);
				if (value.getClass().isArray()) {
					if (value instanceof boolean[]) {
						Arrays.toString((boolean[])value);
					} else if (value instanceof char[]) {
						Arrays.toString((char[])value);
					} else if (value instanceof byte[]) {
						Arrays.toString((byte[])value);
					} else if (value instanceof short[]) {
						Arrays.toString((short[])value);
					} else if (value instanceof int[]) {
						Arrays.toString((int[])value);
					} else if (value instanceof long[]) {
						Arrays.toString((long[])value);
					} else if (value instanceof float[]) {
						Arrays.toString((float[])value);
					} else if (value instanceof double[]) {
						Arrays.toString((double[])value);
					} else {
						Arrays.toString((Object[])value);
					}
				} else {
					sb.append(value);
				}
			} catch (Exception e) {
				sb.append("?");
			}
		}
		sb.append("]");

		return sb.toString();
	}

	private ClassUtil() {
	}

	private static class ContextObjectInputStream extends ObjectInputStream {
		public ContextObjectInputStream(InputStream in) throws IOException {
			super(in);
		}

		@Override
		protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			try {
				return Class.forName(desc.getName(), true, cl);
			} catch (Exception e) {
				return super.resolveClass(desc);
			}
		}
	}

	private static class ParameterizedTypeImpl implements ParameterizedType {
		private ParameterizedType parent;
		private Type[] args;

		public ParameterizedTypeImpl(ParameterizedType parent, Type[] args) {
			this.parent = parent;
			this.args = args;
		}

		@Override
		public Type[] getActualTypeArguments() {
			return args;
		}

		@Override
		public Type getRawType() {
			return parent.getRawType();
		}

		@Override
		public Type getOwnerType() {
			return parent.getOwnerType();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((parent == null) ? 0 : parent.hashCode());
			result = prime * result + Arrays.hashCode(args);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ParameterizedTypeImpl other = (ParameterizedTypeImpl) obj;
			if (parent == null) {
				if (other.parent != null)
					return false;
			} else if (!parent.equals(other.parent))
				return false;
			if (!Arrays.equals(args, other.args))
				return false;
			return true;
		}
	}
}
