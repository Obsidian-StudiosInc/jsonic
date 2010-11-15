package net.arnx.jsonic;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;

import net.arnx.jsonic.JSON.Context;

interface Converter {
	Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception;
}

class NullConverter implements Converter {
	public static final Converter INSTANCE = new NullConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) {
		return null;
	}
}

class PlainConverter implements Converter {
	public static final Converter INSTANCE = new PlainConverter();
	
	private static final Map<Class<?>, Object> PRIMITIVE_MAP = new HashMap<Class<?>, Object>(8);
	
	static {
		PRIMITIVE_MAP.put(boolean.class, false);
		PRIMITIVE_MAP.put(byte.class, (byte)0);
		PRIMITIVE_MAP.put(short.class, (short)0);
		PRIMITIVE_MAP.put(int.class, 0);
		PRIMITIVE_MAP.put(long.class, 0l);
		PRIMITIVE_MAP.put(float.class, 0.0f);
		PRIMITIVE_MAP.put(double.class, 0.0);
		PRIMITIVE_MAP.put(char.class, '\0');
	}
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) {
		return value;
	}
	
	public static Object getDefaultValue(Class<?> cls) {
		return PRIMITIVE_MAP.get(cls);
	}
}

class FormatConverter implements Converter {
	public static final Converter INSTANCE = new FormatConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) {
		return json.format(value);
	}
}

class StringSerializableConverter implements Converter {
	public static final Converter INSTANCE = new StringSerializableConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof String) {
			try {
				Constructor<?> con = c.getConstructor(String.class);
				return con.newInstance(value.toString());
			} catch (NoSuchMethodException e) {
				return null;
			}
		} else if (value != null) {
			throw new UnsupportedOperationException();
		}
		return null;
	}	
}

class SerializableConverter implements Converter {
	public static final Converter INSTANCE = new SerializableConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof String) {
			return deserialize(Base64.decode((String)value));
		} else if (value != null) {
			throw new UnsupportedOperationException();
		}
		return null;
	}
	
	static Object deserialize(byte[] array) throws IOException, ClassNotFoundException {
		Object ret = null;
		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(new ByteArrayInputStream(array));
			ret = in.readObject();
		} finally {
			if (in != null) in.close();
		}
		return ret;
	}
}

class BooleanConverter implements Converter {
	public static final Converter INSTANCE = new BooleanConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		
		if (value instanceof Boolean) {
			return value;
		} else if (value instanceof BigDecimal) {
			return !value.equals(BigDecimal.ZERO);
		} else if (value instanceof BigInteger) {
			return !value.equals(BigInteger.ZERO);
		} else if (value instanceof Number) {
			return ((Number)value).intValue() != 0;
		} else if (value != null){
			String s = value.toString().trim();
			if (s.length() == 0
				|| s.equalsIgnoreCase("f")
				|| s.equalsIgnoreCase("false")
				|| s.equalsIgnoreCase("no")
				|| s.equalsIgnoreCase("off")
				|| s.equals("NaN")) {
				return false;
			} else {
				return true;
			}
		}
		return PlainConverter.getDefaultValue(c);
	}
}

class CharacterConverter implements Converter {
	public static final Converter INSTANCE = new CharacterConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		
		if (value instanceof Boolean) {
			return (((Boolean)value).booleanValue()) ? '1' : '0';
		} else if (value instanceof BigDecimal) {
			return (char)((BigDecimal)value).intValueExact();
		} else if (value instanceof String) {
			String s = value.toString();
			if (s.length() > 0) {
				return s.charAt(0);
			} else {
				return PlainConverter.getDefaultValue(c);
			}
		} else if (value != null) {
			throw new UnsupportedOperationException();
		}
		return PlainConverter.getDefaultValue(c);
	}
}

class ByteConverter implements Converter {
	public static final Converter INSTANCE = new ByteConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		
		if (value instanceof String) {
			NumberFormat f = context.format(NumberFormat.class);
			if (f != null) value = f.parse((String)value);
		}
		
		if (value instanceof Boolean) {
			return (((Boolean)value).booleanValue()) ? 1 : 0;
		} else if (value instanceof BigDecimal) {
			return ((BigDecimal)value).byteValueExact();
		} else if (value instanceof Number) {
			return ((Number)value).byteValue();
		} else if (value instanceof String) {
			String str = value.toString().trim().toLowerCase();
			if (str.length() > 0) {
				int start = 0;
				if (str.charAt(0) == '+') {
					start++;
				}
				
				int num = 0;
				if (str.startsWith("0x", start)) {
					num = Integer.parseInt(str.substring(start+2), 16);
				} else {
					num = Integer.parseInt(str.substring(start));
				}
				
				return (byte)((num > 127) ? num-256 : num);
			} else {
				return PlainConverter.getDefaultValue(c);
			}
		} else if (value != null) {
			throw new UnsupportedOperationException();
		}
		return PlainConverter.getDefaultValue(c);
	}
}

class ShortConverter implements Converter {
	public static final Converter INSTANCE = new ShortConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		
		if (value instanceof String) {
			NumberFormat f = context.format(NumberFormat.class);
			if (f != null) value = f.parse((String)value);
		}
		
		if (value instanceof Boolean) {
			return (((Boolean)value).booleanValue()) ? 1 : 0;
		} else if (value instanceof BigDecimal) {
			return ((BigDecimal)value).shortValueExact();
		} else if (value instanceof Number) {
			return ((Number)value).shortValue();
		} else  if (value instanceof String) {
			String str = value.toString().trim();
			if (str.length() > 0) {
				int start = 0;
				if (str.charAt(0) == '+') {
					start++;
				}
				
				if (str.startsWith("0x", start)) {
					return (short)Integer.parseInt(str.substring(start+2), 16);
				} else {
					return (short)Integer.parseInt(str.substring(start));
				}
			} else {
				return PlainConverter.getDefaultValue(c);
			}
		} else if (value != null) {
			throw new UnsupportedOperationException();
		}
		return PlainConverter.getDefaultValue(c);
	}	
}

class IntegerConverter  implements Converter {
	public static final Converter INSTANCE = new IntegerConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		
		if (value instanceof String) {
			NumberFormat f = context.format(NumberFormat.class);
			if (f != null) value = f.parse((String)value);
		}
		
		if (value instanceof Boolean) {
			return (((Boolean)value).booleanValue()) ? 1 : 0;
		} else if (value instanceof BigDecimal) {
			return ((BigDecimal)value).intValueExact();
		} else if (value instanceof Number) {
			return ((Number)value).intValue();
		} else  if (value instanceof String) {
			String str = value.toString().trim();
			if (str.length() > 0) {
				int start = 0;
				if (str.charAt(0) == '+') {
					start++;
				}
				
				if (str.startsWith("0x", start)) {
					return Integer.parseInt(str.substring(start+2), 16);
				} else {
					return Integer.parseInt(str.substring(start));
				}
			} else {
				return PlainConverter.getDefaultValue(c);
			}
		} else if (value != null) {
			throw new UnsupportedOperationException();
		}
		return PlainConverter.getDefaultValue(c);
	}	
}

class LongConverter  implements Converter {
	public static final Converter INSTANCE = new LongConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		
		if (value instanceof String) {
			NumberFormat f = context.format(NumberFormat.class);
			if (f != null) value = f.parse((String)value);
		}
		
		if (value instanceof Boolean) {
			return (((Boolean)value).booleanValue()) ? 1l : 0l;
		} else if (value instanceof BigDecimal) {
			return ((BigDecimal)value).longValueExact();
		} else if (value instanceof Number) {
			return ((Number)value).longValue();
		} else if (value instanceof String) {
			String str = value.toString().trim();
			if (str.length() > 0) {
				int start = 0;
				if (str.charAt(0) == '+') {
					start++;
				}
				
				if (str.startsWith("0x", start)) {
					return Long.parseLong(str.substring(start+2), 16);
				} else {
					return Long.parseLong(str.substring(start));
				}
			} else {
				return PlainConverter.getDefaultValue(c);
			}					
		} else if (value != null) {
			throw new UnsupportedOperationException();
		}
		return PlainConverter.getDefaultValue(c);
	}	
}

class FloatConverter  implements Converter {
	public static final Converter INSTANCE = new FloatConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		
		if (value instanceof String) {
			NumberFormat f = context.format(NumberFormat.class);
			if (f != null) value = f.parse((String)value);
		}
		
		if (value instanceof Boolean) {
			return (((Boolean)value).booleanValue()) ? 1.0f : Float.NaN;
		} else if (value instanceof Number) {
			return ((Number)value).floatValue();
		} else if (value instanceof String) {
			String str = value.toString().trim();
			if (str.length() > 0) {
				return Float.valueOf(str);
			} else {
				return PlainConverter.getDefaultValue(c);
			}					
		} else if (value != null) {
			throw new UnsupportedOperationException();
		}
		return PlainConverter.getDefaultValue(c);
	}	
}

class DoubleConverter  implements Converter {
	public static final Converter INSTANCE = new DoubleConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		
		if (value instanceof String) {
			NumberFormat f = context.format(NumberFormat.class);
			if (f != null) value = f.parse((String)value);
		}
		
		if (value instanceof Boolean) {
			return (((Boolean)value).booleanValue()) ? 1.0 : Double.NaN;
		} else if (value instanceof Number) {
			return ((Number)value).doubleValue();
		} else if (value instanceof String) {
			String str = value.toString().trim();
			if (str.length() > 0) {
				return Double.valueOf(str);
			} else {
				return PlainConverter.getDefaultValue(c);
			}					
		} else if (value != null) {
			throw new UnsupportedOperationException();
		}
		return PlainConverter.getDefaultValue(c);
	}	
}

class BigIntegerConverter  implements Converter {
	public static final Converter INSTANCE = new BigIntegerConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		
		if (value instanceof String) {
			NumberFormat f = context.format(NumberFormat.class);
			if (f != null) value = f.parse((String)value);
		}
		
		if (value instanceof Boolean) {
			return (((Boolean)value).booleanValue()) ? BigInteger.ONE : BigInteger.ZERO;
		} else if (value instanceof BigDecimal) {
			return ((BigDecimal)value).toBigIntegerExact();
		} else if (value instanceof BigInteger) {
			return value;
		} else if (value instanceof Number) {
			return BigInteger.valueOf(((Number)value).longValue());
		} else if (value instanceof String) {
			String str = value.toString().trim();
			if (str.length() > 0) {
				int start = 0;
				if (str.charAt(0) == '+') {
					start++;
				}
				
				if (str.startsWith("0x", start)) {
					return new BigInteger(str.substring(start+2), 16);
				} else {
					return new BigInteger(str.substring(start));
				}
			}
			return null;
		} else if (value != null) {
			throw new UnsupportedOperationException();
		}
		return null;
	}	
}

class BigDecimalConverter  implements Converter {
	public static final Converter INSTANCE = new BigDecimalConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		
		if (value instanceof BigDecimal) {
			return value;
		} else if (value instanceof String) {
			NumberFormat f = context.format(NumberFormat.class);
			if (f != null) value = f.parse((String)value);
			
			String str = value.toString().trim();
			if (str.length() > 0) {
				if (str.charAt(0) == '+') {
					return new BigDecimal(str.substring(1));
				} else {
					return new BigDecimal(str);
				}
			}
			return null;
		} else if (value != null) {
			throw new UnsupportedOperationException();
		}
		return null;
	}	
}

class PatternConverter implements Converter {
	public static final Converter INSTANCE = new PatternConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		
		if (value instanceof String) {
			return Pattern.compile(value.toString());
		} else if (value != null) {
			throw new UnsupportedOperationException();
		}
		return null;
	}
}

class TimeZoneConverter implements Converter {
	public static final Converter INSTANCE = new TimeZoneConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		
		if (value instanceof String) {
			return TimeZone.getTimeZone(value.toString().trim());
		} else if (value != null) {
			throw new UnsupportedOperationException();
		}
		return null;
	}	
}

class LocaleConverter implements Converter {
	public static final Converter INSTANCE = new LocaleConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			if (src.size() == 1) {
				return new Locale(src.get(0).toString());
			} else if (src.size() == 2) {
				return new Locale(src.get(0).toString(), src.get(1).toString());
			} else if (src.size() > 2) {
				return new Locale(src.get(0).toString(), src.get(1).toString(), src.get(2).toString());
			} else {
				return null;
			}
		} else {
			if (value instanceof Map<?, ?>) {
				value = ((Map<?,?>)value).get(null);
			}
			
			if (value instanceof String) {
				String[] array = value.toString().split("\\p{Punct}");
				
				if (array.length == 1) {
					return new Locale(array[0]);
				} else if (array.length == 2) {
					return new Locale(array[0], array[1]);
				} else if (array.length > 2) {
					return new Locale(array[0], array[1], array[2]);
				} else {
					return null;
				}
			} else if (value != null) {
				throw new UnsupportedOperationException();
			}
		}
		return null;
	}
}

class FileConverter implements Converter {
	public static final Converter INSTANCE = new FileConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		if (value instanceof String) {
			return new File(value.toString().trim());
		} else if (value != null) {
			throw new UnsupportedOperationException();
		}
		return null;
	}
}

class URLConverter implements Converter {
	public static final Converter INSTANCE = new URLConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		if (value instanceof String) {
			if (value instanceof File) {
				return ((File)value).toURI().toURL();
			} else if (value instanceof URI) {
				return ((URI)value).toURL();
			} else {
				return new URL(value.toString().trim());
			}
		} else if (value != null) {
			throw new UnsupportedOperationException();
		}
		return null;
	}
}

class URIConverter implements Converter {
	public static final Converter INSTANCE = new URIConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		if (value instanceof String) {
			if (value instanceof File) {
				return ((File)value).toURI();
			} else if (value instanceof URL) {
				return ((URL)value).toURI();
			} else {
				return new URI(value.toString().trim());
			}
		} else if (value != null) {
			throw new UnsupportedOperationException();
		}
		return null;
	}
}

class UUIDConverter implements Converter {
	public static final Converter INSTANCE = new UUIDConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		if (value instanceof String) {
			return UUID.fromString(value.toString().trim());
		} else if (value != null) {
			throw new UnsupportedOperationException();
		}
		return null;
	}
}

class CharsetConverter implements Converter {
	public static final Converter INSTANCE = new CharsetConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		if (value instanceof String) {
			return Charset.forName(value.toString().trim());
		} else if (value != null) {
			throw new UnsupportedOperationException();
		}
		return null;
	}
}

class ClassConverter implements Converter {
	public static final Converter INSTANCE = new ClassConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		if (value instanceof String) {
			String s = value.toString().trim();
			if (s.equals("boolean")) {
				return boolean.class;
			} else if (s.equals("byte")) {
				return byte.class;
			} else if (s.equals("short")) {
				return short.class;
			} else if (s.equals("int")) {
				return int.class;
			} else if (s.equals("long")) {
				return long.class;
			} else if (s.equals("float")) {
				return float.class;
			} else if (s.equals("double")) {
				return double.class;
			} else {
				try {
					ClassLoader cl = Thread.currentThread().getContextClassLoader();
					return cl.loadClass(value.toString());
				} catch (ClassNotFoundException e) {
					return null;
				}
			}
		} else if (value != null) {
			throw new UnsupportedOperationException();
		}
		return null;
	}
}

class CharSequenceConverter implements Converter {
	public static final Converter INSTANCE = new CharSequenceConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		if (value != null) {
			return value.toString();
		}
		return null;
	}
}

class AppendableConverter implements Converter {
	public static final Converter INSTANCE = new AppendableConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		
		if (value != null) {
			Appendable a = (Appendable)json.create(context, c);
			return a.append(value.toString());
		}
		return null;
	}
}

class EnumConverter implements Converter {
	public static final Converter INSTANCE = new EnumConverter();
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		
		if (value instanceof Number) {
			return c.getEnumConstants()[((Number)value).intValue()];
		} else if (value instanceof Boolean) {
			return c.getEnumConstants()[((Boolean)value) ? 1 : 0];
		} else if (value != null) {
			String str = value.toString().trim();
			if (str.length() == 0) {
				return null;
			} else if (Character.isDigit(str.charAt(0))) {
				return c.getEnumConstants()[Integer.parseInt(str)];
			} else {
				return Enum.valueOf((Class<? extends Enum>)c, str);
			}
		}
		return null;
	}
}

class DateConverter implements Converter {
	public static final Converter INSTANCE = new DateConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		
		Date date = null;
		long millis = -1;
		if (value instanceof Number) {
			millis = ((Number)value).longValue();
			date = (Date)json.create(context, c);
		} else if (value != null) {
			String str = value.toString().trim();
			if (str.length() > 0) {
				millis = convertDate(str, context.getLocale());
				date = (Date)json.create(context, c);						
			}
		}
		
		if (date != null) {
			if (date instanceof java.sql.Date) {
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(millis);
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
				date.setTime(cal.getTimeInMillis());
			} else if (date instanceof java.sql.Time) {
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(millis);
				cal.set(Calendar.YEAR, 1970);
				cal.set(Calendar.MONTH, Calendar.JANUARY);
				cal.set(Calendar.DATE, 1);
				date.setTime(cal.getTimeInMillis());
			} else {
				date.setTime(millis);
			}
		}
		
		return date;
	}
	
	static Long convertDate(String value, Locale locale) throws java.text.ParseException {
		value = value.trim();
		if (value.length() == 0) {
			return null;
		}
		if (locale == null) locale = Locale.getDefault();
		value = Pattern.compile("(?:GMT|UTC)([+-][0-9]{2})([0-9]{2})")
			.matcher(value)
			.replaceFirst("GMT$1:$2");
		
		DateFormat format = null;
		if (Character.isDigit(value.charAt(0))) {
			StringBuilder sb = new StringBuilder(value.length() * 2);

			String types = "yMdHmsSZ";
			// 0: year, 1:month, 2: day, 3: hour, 4: minute, 5: sec, 6:msec, 7: timezone
			int pos = (value.length() > 2 && value.charAt(2) == ':') ? 3 : 0;
			boolean before = true;
			int count = 0;
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				if ((pos == 4 || pos == 5 || pos == 6) 
						&& (c == '+' || c == '-')
						&& (i + 1 < value.length())
						&& (Character.isDigit(value.charAt(i+1)))) {
					
					if (!before) sb.append('\'');
					pos = 7;
					count = 0;
					before = true;
					continue;
				} else if (pos == 7 && c == ':'
						&& (i + 1 < value.length())
						&& (Character.isDigit(value.charAt(i+1)))) {
					value = value.substring(0, i) + value.substring(i+1);
					continue;
				}
				
				boolean digit = (Character.isDigit(c) && pos < 8);
				if (before != digit) {
					sb.append('\'');
					if (digit) {
						count = 0;
						pos++;
					}
				}
				
				if (digit) {
					char type = types.charAt(pos);
					if (count == ((type == 'y' || type == 'Z') ? 4 : (type == 'S') ? 3 : 2)) {
						count = 0;
						pos++;
						type = types.charAt(pos);
					}
					if (type != 'Z' || count == 0) sb.append(type);
					count++;
				} else {
					sb.append((c == '\'') ? "''" : c);
				}
				before = digit;
			}
			if (!before) sb.append('\'');
			
			format = new SimpleDateFormat(sb.toString(), Locale.ENGLISH);
		} else if (value.length() > 18) {
			if (value.charAt(3) == ',') {
				String pattern = "EEE, dd MMM yyyy HH:mm:ss Z";
				format = new SimpleDateFormat(
						(value.length() < pattern.length()) ? pattern.substring(0, value.length()) : pattern, Locale.ENGLISH);
			} else if (value.charAt(13) == ':') {
				format = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
			} else if (value.charAt(18) == ':') {
				String pattern = "EEE MMM dd yyyy HH:mm:ss Z";
				format = new SimpleDateFormat(
						(value.length() < pattern.length()) ? pattern.substring(0, value.length()) : pattern, Locale.ENGLISH);
			} else  {
				format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale);
			}
		} else {
			format = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
		}
		
		return format.parse(value).getTime();
	}
}

class CalendarConverter implements Converter {
	public static final Converter INSTANCE = new CalendarConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		
		if (value instanceof Number) {
			Calendar cal = (Calendar)json.create(context, c);
			cal.setTimeInMillis(((Number)value).longValue());
			return cal;
		} else if (value != null) {
			String str = value.toString().trim();
			if (str.length() > 0) {
				Calendar cal = (Calendar)json.create(context, c);
				cal.setTimeInMillis(DateConverter.convertDate(str, context.getLocale()));
				return  cal;
			}
		}
		return null;
	}
}

class InetAddressConverter implements Converter {
	public static final Converter INSTANCE = new InetAddressConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		
		if (value != null) {
			Class<?> inetAddressClass = ClassUtil.findClass("java.net.InetAddress");
			return inetAddressClass.getMethod("getByName", String.class).invoke(null, value.toString().trim());
		}
		return null;
	}
}

class ArrayConverter implements Converter {
	public static final Converter INSTANCE = new ArrayConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			Map<?, ?> src = (Map<?, ?>)value;
			if (!(src instanceof SortedMap<?, ?>)) {
				src = new TreeMap<Object, Object>(src);
			}
			value = src.values();
		}
		
		if (value instanceof Collection) {
			Collection<?> src = (Collection<?>)value;
			Object array = Array.newInstance(c.getComponentType(), src.size());
			Class<?> pc = c.getComponentType();
			Type pt = (t instanceof GenericArrayType) ? 
					((GenericArrayType)t).getGenericComponentType() : pc;
			
			Iterator<?> it = src.iterator();
			for (int i = 0; it.hasNext(); i++) {
				context.enter(i);
				Array.set(array, i, json.postparse(context, it.next(), pc, pt));
				context.exit();
			}
			return array;
		} else {
			if (value instanceof String && byte.class.equals(c.getComponentType())) {
				return Base64.decode((String)value);
			} else {
				Object array = Array.newInstance(c.getComponentType(), 1);
				Class<?> pc = c.getComponentType();
				Type pt = (t instanceof GenericArrayType) ? 
						((GenericArrayType)t).getGenericComponentType() : pc;
				context.enter(0);
				Array.set(array, 0, json.postparse(context, value, pc, pt));
				context.exit();
				return array;
			}
		}
	}
}

class CollectionConverter implements Converter {
	public static final Converter INSTANCE = new CollectionConverter();
	
	@SuppressWarnings("unchecked")
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map) {
			Map<?, ?> src = (Map<?, ?>)value;
			if (!(src instanceof SortedMap<?, ?>)) {
				src = new TreeMap<Object, Object>(src);
			}
			value = src.values();
		}
		
		if (value instanceof Collection) {
			Collection<?> src = (Collection<?>)value;
			Collection<Object> collection = null;
			if (t instanceof ParameterizedType) {
				Type[] pts = ((ParameterizedType)t).getActualTypeArguments();
				Type pt = (pts != null && pts.length > 0) ? pts[0] : Object.class;
				Class<?> pc = ClassUtil.getRawType(pt);
				
				if (Object.class.equals(pc)) {
					collection = (Collection<Object>)src;
				} else {
					collection = (Collection<Object>)json.create(context, c);
					Iterator<?> it = src.iterator();
					for (int i = 0; it.hasNext(); i++) {
						context.enter(i);
						collection.add(json.postparse(context, it.next(), pc, pt));
						context.exit();
					}
				}
			} else {
				collection = (Collection<Object>)json.create(context, c);
				collection.addAll(src);
			}
			return collection;
		} else {
			Collection<Object> collection = (Collection<Object>)json.create(context, c);
			if (t instanceof ParameterizedType) {
				Type[] pts = ((ParameterizedType)t).getActualTypeArguments();
				Type pt = (pts != null && pts.length > 0) ? pts[0] : Object.class;
				Class<?> pc = ClassUtil.getRawType(pt);
				context.enter(0);
				collection.add(json.postparse(context, value, pc, pt));
				context.exit();
			} else {
				collection.add(value);
			}
			return collection;
		}
	}	
}

class MapConverter implements Converter {
	public static final Converter INSTANCE = new MapConverter();
	
	@SuppressWarnings("unchecked")
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			Map<Object, Object> map = null;
			if (Properties.class.isAssignableFrom(c)) {
				map = (Map<Object, Object>)json.create(context, c);
				flattenProperties(new StringBuilder(32), (Map<Object, Object>)value, (Properties)map);
			} else if (t instanceof ParameterizedType) {
				Type[] pts = ((ParameterizedType)t).getActualTypeArguments();
				Type pt0 = (pts != null && pts.length > 0) ? pts[0] : Object.class;
				Type pt1 = (pts != null && pts.length > 1) ? pts[1] : Object.class;
				Class<?> pc0 = ClassUtil.getRawType(pt0);
				Class<?> pc1 = ClassUtil.getRawType(pt1);
				
				if ((Object.class.equals(pc0) || String.class.equals(pc0))
						&& Object.class.equals(pc1)) {
					map = (Map<Object, Object>)value;
				} else {
					map = (Map<Object, Object>)json.create(context, c);
					for (Map.Entry<?, ?> entry : ((Map<?,?>)value).entrySet()) {
						context.enter('.');
						Object key = json.postparse(context, entry.getKey(), pc0, pt0);
						context.exit();
						
						context.enter(entry.getKey());
						map.put(key, json.postparse(context, entry.getValue(), pc1, pt1));
						context.exit();
					}
				}
			} else {
				map = (Map<Object, Object>)json.create(context, c);
				map.putAll((Map<?,?>)value);
			}
			return map;
		} else if (value instanceof List<?>) {
			Map<Object, Object> map = (Map<Object, Object>)json.create(context, c);
			if (Properties.class.isAssignableFrom(c)) {
				flattenProperties(new StringBuilder(32), (List<Object>)value, (Properties)map);
			} else if (t instanceof ParameterizedType) {
				Type[] pts = ((ParameterizedType)t).getActualTypeArguments();
				Type pt0 = (pts != null && pts.length > 0) ? pts[0] : Object.class;
				Type pt1 = (pts != null && pts.length > 1) ? pts[1] : Object.class;
				Class<?> pc0 = ClassUtil.getRawType(pt0);
				Class<?> pc1 = ClassUtil.getRawType(pt1);
				
				List<?> src = (List<?>)value;
				for (int i = 0; i < src.size(); i++) {
					context.enter('.');
					Object key = json.postparse(context, i, pc0, pt0);
					context.exit();
					
					context.enter(i);
					map.put(key, json.postparse(context, src.get(i), pc1, pt1));
					context.exit();
				}
			} else {
				List<?> src = (List<?>)value;
				for (int i = 0; i < src.size(); i++) {
					map.put(i, src.get(i));
				}
			}
			return map;
		} else {
			JSONHint hint = context.getHint();
			
			Map<Object, Object> map = (Map<Object, Object>)json.create(context, c);
			Object key = (hint != null && hint.anonym().length() > 0) ? hint.anonym() : null;
			if (t instanceof ParameterizedType) {
				Type[] pts = ((ParameterizedType)t).getActualTypeArguments();
				Type pt0 = (pts != null && pts.length > 0) ? pts[0] : Object.class;
				Type pt1 = (pts != null && pts.length > 1) ? pts[1] : Object.class;
				Class<?> pc0 = ClassUtil.getRawType(pt0);
				Class<?> pc1 = ClassUtil.getRawType(pt1);
				
				context.enter('.');
				key = json.postparse(context, key, pc0, pt0);
				context.exit();
				
				context.enter(key);
				map.put(key, json.postparse(context, value, pc1, pt1));
				context.exit();
			} else {
				map.put(value, null);
			}
			return map;
		}
	}
	
	private static void flattenProperties(StringBuilder key, Object value, Properties props) {
		if (value instanceof Map<?,?>) {
			for (Map.Entry<?, ?> entry : ((Map<?, ?>)value).entrySet()) {
				int pos = key.length();
				if (pos > 0) key.append('.');
				key.append(entry.getKey());
				flattenProperties(key, entry.getValue(), props);
				key.setLength(pos);
			}
		} else if (value instanceof List<?>) {
			List<?> list = (List<?>)value;
			for (int i = 0; i < list.size(); i++) {
				int pos = key.length();
				if (pos > 0) key.append('.');
				key.append(i);
				flattenProperties(key, list.get(i), props);
				key.setLength(pos);
			}
		} else {
			props.setProperty(key.toString(), value.toString());
		}
	}
}

class ObjectConverter implements Converter {
	public static final Converter INSTANCE = new ObjectConverter();
	
	public Object convert(JSON json, Context context, Object value, Class<?> c, Type t) throws Exception {
		Map<String, AnnotatedElement> props = context.getSetProperties(c);
		if (value instanceof Map<?, ?>) {
			Object o = json.create(context, c);
			if (o == null) return null;
			for (Map.Entry<?, ?> entry : ((Map<?, ?>)value).entrySet()) {
				String name = entry.getKey().toString();
				AnnotatedElement target = props.get(name);
				if (target == null) target = props.get(ClassUtil.toLowerCamel(name));
				if (target == null) continue;
				
				context.enter(name, target.getAnnotation(JSONHint.class));
				if (target instanceof Method) {
					Method m = (Method)target;
					Type gptype = m.getGenericParameterTypes()[0];
					Class<?> ptype = m.getParameterTypes()[0];
					if (gptype instanceof TypeVariable<?> && t instanceof ParameterizedType) {
						gptype = ClassUtil.resolveTypeVariable((TypeVariable<?>)gptype, (ParameterizedType)t);
						ptype = ClassUtil.getRawType(gptype);
					}
					m.invoke(o, json.postparse(context, entry.getValue(), ptype, gptype));
				} else {
					Field f = (Field)target;
					Type gptype = f.getGenericType();
					Class<?> ptype =  f.getType();
					if (gptype instanceof TypeVariable<?> && t instanceof ParameterizedType) {
						gptype = ClassUtil.resolveTypeVariable((TypeVariable<?>)gptype, (ParameterizedType)t);
						ptype = ClassUtil.getRawType(gptype);
					}
					
					f.set(o, json.postparse(context, entry.getValue(), ptype, gptype));
				}
				context.exit();
			}
			return o;
		} else if (value instanceof List<?>) {
			throw new UnsupportedOperationException();
		} else {
			JSONHint hint = context.getHint();
			if (hint != null && hint.anonym().length() > 0) {
				AnnotatedElement target = props.get(hint.anonym());
				if (target == null) return null;
				Object o = json.create(context, c);
				if (o == null) return null;
				context.enter(hint.anonym(), target.getAnnotation(JSONHint.class));
				if (target instanceof Method) {
					Method m = (Method)target;
					Type gptype = m.getGenericParameterTypes()[0];
					Class<?> ptype = m.getParameterTypes()[0];
					if (gptype instanceof TypeVariable<?> && t instanceof ParameterizedType) {
						gptype = ClassUtil.resolveTypeVariable((TypeVariable<?>)gptype, (ParameterizedType)t);
						ptype = ClassUtil.getRawType(gptype);
					}
					m.invoke(o, json.postparse(context, value, ptype, gptype));
				} else {
					Field f = (Field)target;
					Type gptype = f.getGenericType();
					Class<?> ptype =  f.getType();
					if (gptype instanceof TypeVariable<?> && t instanceof ParameterizedType) {
						gptype = ClassUtil.resolveTypeVariable((TypeVariable<?>)gptype, (ParameterizedType)t);
						ptype = ClassUtil.getRawType(gptype);
					}
					
					f.set(o, json.postparse(context, value, ptype, gptype));
				}
				context.exit();
				return o;
			} else {
				throw new UnsupportedOperationException();
			}
		}
	}	
}