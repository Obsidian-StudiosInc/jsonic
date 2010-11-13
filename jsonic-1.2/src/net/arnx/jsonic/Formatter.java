package net.arnx.jsonic;

import java.io.ByteArrayOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.JSON.Mode;

interface Formatter {
	boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws Exception;
}

class NullFormatter implements Formatter {
	public static Formatter INSTANCE = new NullFormatter();

	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		in.append("null");
		return false;
	}
}

class PlainFormatter implements Formatter {
	public static Formatter INSTANCE = new PlainFormatter();

	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		in.append(o.toString());
		return false;
	}
}

class StringFormatter implements Formatter {
	public static Formatter INSTANCE = new StringFormatter();

	private static final int[] ESCAPE_CHARS = new int[128];

	static {
		for (int i = 0; i < 32; i++) {
			ESCAPE_CHARS[i] = -1;
		}
		ESCAPE_CHARS['\b'] = 'b';
		ESCAPE_CHARS['\t'] = 't';
		ESCAPE_CHARS['\n'] = 'n';
		ESCAPE_CHARS['\f'] = 'f';
		ESCAPE_CHARS['\r'] = 'r';
		ESCAPE_CHARS['"'] = '"';
		ESCAPE_CHARS['\\'] = '\\';
		ESCAPE_CHARS['<'] = -2;
		ESCAPE_CHARS['>'] = -2;
		ESCAPE_CHARS[0x7F] = -1;
	}


	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		serialize(context, o.toString(), in);
		return false;
	}

	static void serialize(Context context, String s, InputSource in)
			throws IOException {
		in.append('"');
		int start = 0;
		int length = s.length();
		for (int i = 0; i < length; i++) {
			int c = s.charAt(i);
			if (c < ESCAPE_CHARS.length && ESCAPE_CHARS[c] != 0) {
				int x = ESCAPE_CHARS[c];
				if (x > 0) {
					if (start < i)
						in.append(s, start, i);
					in.append('\\');
					in.append((char) x);
					start = i + 1;
				} else if (x == -1 || (x == -2 && context.getMode() == Mode.SCRIPT)) {
					if (start < i)
						in.append(s, start, i);
					in.append("\\u00");
					in.append("0123456789ABCDEF".charAt(c / 16));
					in.append("0123456789ABCDEF".charAt(c % 16));
					start = i + 1;
				}
			}
		}
		if (start < length)
			in.append(s, start, length);
		in.append('"');
	}
}

class NumberFormatter implements Formatter {
	public static Formatter INSTANCE = new NumberFormatter();

	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		NumberFormat f = context.format(NumberFormat.class);
		if (f != null) {
			StringFormatter.serialize(context, f.format(o), in);
		} else {
			in.append(o.toString());
		}
		return false;
	}
}

class FloatFormatter implements Formatter {
	public static Formatter INSTANCE = new FloatFormatter();

	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		NumberFormat f = context.format(NumberFormat.class);
		if (f != null) {
			StringFormatter.serialize(context, f.format(o), in);
		} else {
			double d = ((Number) o).doubleValue();
			if (Double.isNaN(d) || Double.isInfinite(d)) {
				if (context.getMode() != Mode.SCRIPT) {
					in.append('"');
					in.append(o.toString());
					in.append('"');
				} else if (Double.isNaN(d)) {
					in.append("Number.NaN");
				} else {
					in.append("Number.");
					in.append((d > 0) ? "POSITIVE" : "NEGATIVE");
					in.append("_INFINITY");
				}
			} else {
				in.append(o.toString());
			}
		}
		return false;
	}
}

class DateFormatter implements Formatter {
	public static Formatter INSTANCE = new DateFormatter();

	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		Date date = (Date) o;
		DateFormat f = context.format(DateFormat.class);
		if (f != null) {
			StringFormatter.serialize(context, f.format(o), in);
		} else if (context.getMode() == Mode.SCRIPT) {
			in.append("new Date(");
			in.append(Long.toString(date.getTime()));
			in.append(")");
		} else {
			in.append(Long.toString(date.getTime()));
		}
		return false;
	}
}

class BooleanArrayFormatter implements Formatter {
	public static Formatter INSTANCE = new BooleanArrayFormatter();

	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		in.append('[');
		boolean[] array = (boolean[]) o;
		for (int i = 0; i < array.length; i++) {
			in.append(String.valueOf(array[i]));
			if (i != array.length - 1) {
				in.append(',');
				if (context.isPrettyPrint())
					in.append(' ');
			}
		}
		in.append(']');
		return true;
	}
}

class ByteArrayFormatter implements Formatter {
	public static Formatter INSTANCE = new ByteArrayFormatter();

	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		StringFormatter.serialize(context, Base64.encode((byte[]) o), in);
		return false;
	}
}

class SerializableFormatter extends StringFormatter {
	public static Formatter INSTANCE = new SerializableFormatter();
	
	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		return super.format(json, context, src,
				Base64.encode(serialize(o)), in);
	}
	
	static byte[] serialize(Object data) throws IOException {
		ByteArrayOutputStream array = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(array);
		out.writeObject(data);
		out.close();
		return array.toByteArray();
	}
}

class ShortArrayFormatter implements Formatter {
	public static Formatter INSTANCE = new ShortArrayFormatter();
	
	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		NumberFormat f = context.format(NumberFormat.class);
		short[] array = (short[]) o;
		in.append('[');
		for (int i = 0; i < array.length; i++) {
			if (f != null) {
				StringFormatter.serialize(context, f.format(array[i]), in);
			} else {
				in.append(String.valueOf(array[i]));
			}
			if (i != array.length - 1) {
				in.append(',');
				if (context.isPrettyPrint())
					in.append(' ');
			}
		}
		in.append(']');
		return true;
	}
}

class IntArrayFormatter implements Formatter {
	public static Formatter INSTANCE = new IntArrayFormatter();

	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		NumberFormat f = context.format(NumberFormat.class);
		int[] array = (int[]) o;
		in.append('[');
		for (int i = 0; i < array.length; i++) {
			if (f != null) {
				StringFormatter.serialize(context, f.format(array[i]), in);
			} else {
				in.append(String.valueOf(array[i]));
			}
			if (i != array.length - 1) {
				in.append(',');
				if (context.isPrettyPrint())
					in.append(' ');
			}
		}
		in.append(']');
		return true;
	}
}

class LongArrayFormatter implements Formatter {
	public static Formatter INSTANCE = new LongArrayFormatter();
	
	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		NumberFormat f = context.format(NumberFormat.class);
		long[] array = (long[]) o;
		in.append('[');
		for (int i = 0; i < array.length; i++) {
			if (f != null) {
				StringFormatter.serialize(context, f.format(array[i]), in);
			} else {
				in.append(String.valueOf(array[i]));
			}
			if (i != array.length - 1) {
				in.append(',');
				if (context.isPrettyPrint())
					in.append(' ');
			}
		}
		in.append(']');
		return true;
	}
}

class FloatArrayFormatter implements Formatter {
	public static Formatter INSTANCE = new FloatArrayFormatter();
	
	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		NumberFormat f = context.format(NumberFormat.class);
		float[] array = (float[]) o;
		in.append('[');
		for (int i = 0; i < array.length; i++) {
			if (Float.isNaN(array[i]) || Float.isInfinite(array[i])) {
				if (context.getMode() != Mode.SCRIPT) {
					in.append('"');
					in.append(Float.toString(array[i]));
					in.append('"');
				} else if (Double.isNaN(array[i])) {
					in.append("Number.NaN");
				} else {
					in.append("Number.");
					in.append((array[i] > 0) ? "POSITIVE" : "NEGATIVE");
					in.append("_INFINITY");
				}
			} else if (f != null) {
				StringFormatter.serialize(context, f.format(array[i]), in);
			} else {
				in.append(String.valueOf(array[i]));
			}
			if (i != array.length - 1) {
				in.append(',');
				if (context.isPrettyPrint())
					in.append(' ');
			}
		}
		in.append(']');
		return true;
	}
}

class DoubleArrayFormatter implements Formatter {
	public static Formatter INSTANCE = new DoubleArrayFormatter();
	
	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		NumberFormat f = context.format(NumberFormat.class);
		double[] array = (double[]) o;
		in.append('[');
		for (int i = 0; i < array.length; i++) {
			if (Double.isNaN(array[i]) || Double.isInfinite(array[i])) {
				if (context.getMode() != Mode.SCRIPT) {
					in.append('"');
					in.append(Double.toString(array[i]));
					in.append('"');
				} else if (Double.isNaN(array[i])) {
					in.append("Number.NaN");
				} else {
					in.append("Number.");
					in.append((array[i] > 0) ? "POSITIVE" : "NEGATIVE");
					in.append("_INFINITY");
				}
			} else if (f != null) {
				StringFormatter.serialize(context, f.format(array[i]), in);
			} else {
				in.append(String.valueOf(array[i]));
			}
			if (i != array.length - 1) {
				in.append(',');
				if (context.isPrettyPrint())
					in.append(' ');
			}
		}
		in.append(']');
		return true;
	}
}

class ObjectArrayFormatter implements Formatter {
	public static Formatter INSTANCE = new ObjectArrayFormatter();
	
	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		Object[] array = (Object[]) o;
		in.append('[');
		int i = 0;
		for (; i < array.length; i++) {
			Object item = array[i];
			if (item == src)
				item = null;

			if (i != 0)
				in.append(',');
			if (context.isPrettyPrint()) {
				in.append('\n');
				for (int j = 0; j < context.getLevel() + 1; j++)
					in.append('\t');
			}
			context.enter(i);
			json.format(context, item, in);
			context.exit();
		}
		if (context.isPrettyPrint() && i > 0) {
			in.append('\n');
			for (int j = 0; j < context.getLevel(); j++)
				in.append('\t');
		}
		in.append(']');
		return true;
	}
}


class ByteFormatter extends PlainFormatter {
	public static Formatter INSTANCE = new ByteFormatter();
	
	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		return super.format(json, context, src,
				Integer.toString(((Byte) o).byteValue() & 0xFF), in);
	}
}

class ClassFormatter extends StringFormatter {
	public static Formatter INSTANCE = new ClassFormatter();
	
	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		return super.format(json, context, src, ((Class<?>) o).getName(), in);
	}
}

class LocaleFormatter extends StringFormatter {
	public static Formatter INSTANCE = new LocaleFormatter();
	
	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		return super.format(json, context, src, ((Locale) o).toString()
				.replace('_', '-'), in);
	}
}

class CharArrayFormatter extends StringFormatter {
	public static Formatter INSTANCE = new CharArrayFormatter();
	
	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		return super.format(json, context, src, new String((char[]) o), in);
	}
}

class ListFormatter implements Formatter {
	public static Formatter INSTANCE = new ListFormatter();
	
	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		List<?> list = (List<?>) o;
		in.append('[');
		int length = list.size();
		int i = 0;
		for (; i < length; i++) {
			Object item = list.get(i);
			if (item == src) item = null;

			if (i != 0) in.append(',');
			if (context.isPrettyPrint()) {
				in.append('\n');
				for (int j = 0; j < context.getLevel() + 1; j++)
					in.append('\t');
			}
			context.enter(i);
			json.format(context, item, in);
			context.exit();
		}
		if (context.isPrettyPrint() && i > 0) {
			in.append('\n');
			for (int j = 0; j < context.getLevel(); j++)
				in.append('\t');
		}
		in.append(']');
		return true;
	}
}

class IteratorFormatter implements Formatter {
	public static Formatter INSTANCE = new IteratorFormatter();
	
	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		Iterator<?> t = (Iterator<?>) o;
		in.append('[');
		int i = 0;
		for (; t.hasNext(); i++) {
			Object item = t.next();
			if (item == src)
				item = null;

			if (i != 0)
				in.append(',');
			if (context.isPrettyPrint()) {
				in.append('\n');
				for (int j = 0; j < context.getLevel() + 1; j++)
					in.append('\t');
			}
			context.enter(i);
			json.format(context, item, in);
			context.exit();
		}
		if (context.isPrettyPrint() && i > 0) {
			in.append('\n');
			for (int j = 0; j < context.getLevel(); j++)
				in.append('\t');
		}
		in.append(']');
		return true;
	}
}

class IterableFormatter extends IteratorFormatter {
	public static Formatter INSTANCE = new IterableFormatter();
	
	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		return super.format(json, context, src, ((Iterable<?>) o).iterator(),
				in);
	}
}

class EnumerationFormatter implements Formatter {
	public static Formatter INSTANCE = new EnumerationFormatter();
	
	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		Enumeration<?> e = (Enumeration<?>) o;
		in.append('[');
		int i = 0;
		for (; e.hasMoreElements(); i++) {
			Object item = e.nextElement();
			if (item == src)
				item = null;

			if (i != 0)
				in.append(',');
			if (context.isPrettyPrint()) {
				in.append('\n');
				for (int j = 0; j < context.getLevel() + 1; j++)
					in.append('\t');
			}
			context.enter(i);
			json.format(context, item, in);
			context.exit();
		}
		if (context.isPrettyPrint() && i > 0) {
			in.append('\n');
			for (int j = 0; j < context.getLevel(); j++)
				in.append('\t');
		}
		in.append(']');
		return true;
	}
}

class MapFormatter implements Formatter {
	public static Formatter INSTANCE = new MapFormatter();
	
	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		Map<?, ?> map = (Map<?, ?>) o;

		in.append('{');
		int i = 0;
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			Object key = entry.getKey();
			if (key == null)
				continue;

			Object value = entry.getValue();
			if (value == src || (context.isSuppressNull() && value == null))
				continue;

			if (i > 0)
				in.append(',');
			if (context.isPrettyPrint()) {
				in.append('\n');
				for (int j = 0; j < context.getLevel() + 1; j++)
					in.append('\t');
			}
			StringFormatter.serialize(context, key.toString(), in);
			in.append(':');
			if (context.isPrettyPrint())
				in.append(' ');
			context.enter(key);
			json.format(context, value, in);
			context.exit();
			i++;
		}
		if (context.isPrettyPrint() && i > 0) {
			in.append('\n');
			for (int j = 0; j < context.getLevel(); j++)
				in.append('\t');
		}
		in.append('}');
		return true;
	}
}

class ObjectFormatter implements Formatter {
	public static Formatter INSTANCE = new ObjectFormatter();
	
	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws Exception {
		List<Property> props = context.getGetProperties(o.getClass());

		in.append('{');
		int i = 0;
		int length = props.size();
		for (int p = 0; p < length; p++) {
			Property prop = props.get(p);
			Object value = null;
			Exception cause = null;

			try {
				value = prop.get(o);
				if (value == src || (context.isSuppressNull() && value == null))
					continue;

				if (i > 0)
					in.append(',');
				if (context.isPrettyPrint()) {
					in.append('\n');
					for (int j = 0; j < context.getLevel() + 1; j++)
						in.append('\t');
				}
			} catch (Exception e) {
				cause = e;
			}

			StringFormatter.serialize(context, prop.getName(), in);
			in.append(':');
			if (context.isPrettyPrint())
				in.append(' ');
			context.enter(prop.getName(), prop.getHint());
			if (cause != null) throw cause;
			
			json.format(context, value, in);
			context.exit();
			i++;
		}
		if (context.isPrettyPrint() && i > 0) {
			in.append('\n');
			for (int j = 0; j < context.getLevel(); j++)
				in.append('\t');
		}
		in.append('}');
		return true;
	}
}

class DynaBeanFormatter implements Formatter {
	public static Formatter INSTANCE = new DynaBeanFormatter();
	
	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		in.append('{');
		int i = 0;
		try {
			Class<?> dynaBeanClass = ClassUtil
					.findClass("org.apache.commons.beanutils.DynaBean");

			Object dynaClass = dynaBeanClass.getMethod("getDynaClass")
					.invoke(o);
			Object[] dynaProperties = (Object[]) dynaClass.getClass()
					.getMethod("getDynaProperties").invoke(dynaClass);

			if (dynaProperties != null && dynaProperties.length > 0) {
				Method getName = dynaProperties[0].getClass().getMethod(
						"getName");
				Method get = dynaBeanClass.getMethod("get", String.class);

				for (Object dp : dynaProperties) {
					Object name = null;
					try {
						name = getName.invoke(dp);
					} catch (Exception e) {
					}
					if (name == null)
						continue;

					Object value = null;
					Exception cause = null;

					try {
						value = get.invoke(o, name);
					} catch (Exception e) {
						cause = e;
					}

					if (value == src
							|| (cause == null && context.isSuppressNull() && value == null))
						continue;

					if (i > 0)
						in.append(',');
					if (context.isPrettyPrint()) {
						in.append('\n');
						for (int j = 0; j < context.getLevel() + 1; j++)
							in.append('\t');
					}
					StringFormatter.serialize(context, name.toString(), in);
					in.append(':');
					if (context.isPrettyPrint())
						in.append(' ');
					context.enter(name);
					if (cause != null) {
						throw new JSONException(json.getMessage(
								"json.format.ConversionError",
								(src instanceof CharSequence) ? "\"" + src
										+ "\"" : src, context),
								JSONException.FORMAT_ERROR, cause);
					}
					json.format(context, value, in);
					context.exit();
					i++;
				}
			}
		} catch (Exception e) {
			// no handle
		}
		if (context.isPrettyPrint() && i > 0) {
			in.append('\n');
			for (int j = 0; j < context.getLevel(); j++)
				in.append('\t');
		}
		in.append('}');
		return true;
	}
}

class DOMElementFormatter implements Formatter {
	public static Formatter INSTANCE = new DOMElementFormatter();
	
	public boolean format(JSON json, Context context, Object src, Object o,
			InputSource in) throws IOException {
		Element elem = (Element) o;
		in.append('[');
		StringFormatter.serialize(context, elem.getTagName(), in);

		in.append(',');
		if (context.isPrettyPrint()) {
			in.append('\n');
			for (int j = 0; j < context.getLevel() + 1; j++)
				in.append('\t');
		}
		in.append('{');
		if (elem.hasAttributes()) {
			NamedNodeMap names = elem.getAttributes();
			for (int i = 0; i < names.getLength(); i++) {
				if (i != 0) {
					in.append(',');
				}
				if (context.isPrettyPrint() && names.getLength() > 1) {
					in.append('\n');
					for (int j = 0; j < context.getLevel() + 2; j++)
						in.append('\t');
				}
				Node node = names.item(i);
				if (node instanceof Attr) {
					StringFormatter.serialize(context, node.getNodeName(), in);
					in.append(':');
					if (context.isPrettyPrint())
						in.append(' ');
					StringFormatter.serialize(context, node.getNodeValue(), in);
				}
			}
			if (context.isPrettyPrint() && names.getLength() > 1) {
				in.append('\n');
				for (int j = 0; j < context.getLevel() + 1; j++)
					in.append('\t');
			}
		}
		in.append('}');
		if (elem.hasChildNodes()) {
			NodeList nodes = elem.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				if ((node instanceof Element)
						|| (node instanceof CharacterData && !(node instanceof Comment))) {
					in.append(',');
					if (context.isPrettyPrint()) {
						in.append('\n');
						for (int j = 0; j < context.getLevel() + 1; j++)
							in.append('\t');
					}
					context.enter(i + 2);
					json.format(context, node, in);
					context.exit();
					if (in instanceof Flushable)
						((Flushable) in).flush();
				}
			}
		}
		if (context.isPrettyPrint()) {
			in.append('\n');
			for (int j = 0; j < context.getLevel(); j++)
				in.append('\t');
		}
		in.append(']');
		return true;
	}
}
