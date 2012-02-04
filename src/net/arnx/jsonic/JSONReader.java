package net.arnx.jsonic;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.io.InputSource;
import net.arnx.jsonic.parse.ParseContext;
import net.arnx.jsonic.parse.Parser;
import net.arnx.jsonic.parse.ScriptParser;
import net.arnx.jsonic.parse.StrictParser;
import net.arnx.jsonic.parse.TraditionalParser;

public class JSONReader {
	private Context context;
	private Parser parser;
	private JSONEventType type;
	
	JSONReader(Context context, InputSource in, boolean multilineMode, boolean ignoreWhitespace) {
		this.context = context;
		
		ParseContext pcontext = new ParseContext(context, multilineMode, ignoreWhitespace);
		switch (context.getMode()) {
		case STRICT:
			parser = new StrictParser(in, pcontext);
			break;
		case SCRIPT:
			parser = new ScriptParser(in, pcontext);
			break;
		default:
			parser = new TraditionalParser(in, pcontext);
		}
	}
	
	public JSONEventType next() throws IOException {
		type = parser.next();
		return type;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getValue(Class<T> cls) throws IOException {
		return (T)context.convertInternal(getValue(), cls);
	}
	
	public Object getValue(Type t) throws IOException {
		return context.convertInternal(getValue(), t);
	}
	
	public Map<?, ?> getObject() throws IOException {
		if (type == JSONEventType.START_OBJECT) {
			return (Map<?, ?>)getValue();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public List<?> getArray() throws IOException {
		if (type == JSONEventType.START_ARRAY) {
			return (List<?>)getValue();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public String getString() throws IOException {
		if (type == JSONEventType.STRING) {
			return (String)parser.getValue();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public BigDecimal getNumber() throws IOException {
		if (type == JSONEventType.NUMBER) {
			return (BigDecimal)parser.getValue();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public Boolean getBoolean() throws IOException {
		if (type == JSONEventType.BOOLEAN) {
			return (Boolean)parser.getValue();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public String getComment() throws IOException {
		if (type == JSONEventType.COMMENT) {
			return (String)parser.getValue();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public String getWhitespace() throws IOException {
		if (type == JSONEventType.WHITESPACE) {
			return (String)parser.getValue();
		} else {
			throw new IllegalStateException();
		}
	}
	
	Object getValue() throws IOException {
		if (type == null) {
			throw new IllegalStateException("you should call next.");
		}
		
		int ilen = 0;
		int[] istack = new int[16];
		
		int olen = 0;
		Object[] ostack = new Object[32];
		
		JSONEventType btype = null;
		do {
			switch (type) {
			case START_OBJECT:
			case START_ARRAY:
				istack = iexpand(istack, ilen + 1);
				istack[ilen++] = olen;
				break;
			case NAME:
			case STRING:
			case NUMBER:
			case BOOLEAN:
			case NULL:
				Object value = parser.getValue();
				if (value == null && context.isSuppressNull() && btype == JSONEventType.NAME) {
					olen--;
				} else {
					ostack = oexpand(ostack, olen + 1);
					ostack[olen++] = value;
				}
				break;
			case END_ARRAY: {
				int start = istack[--ilen];
				int len = olen - start;
				List<Object> array = new ArrayList<Object>(len);
				for (int i = start; i < olen; i++) {
					array.add(ostack[i]);
				}
				olen = start;
				ostack = oexpand(ostack, olen + 1);
				ostack[olen++] = array;
				break;
			}
			case END_OBJECT:
				int start = istack[--ilen];
				int len = olen - start;
				Map<Object, Object> object = new LinkedHashMap<Object, Object>(
						(len < 2) ? 4 : 
						(len < 4) ? 8 : 
						(len < 12) ? 16 : 
						(int)(len / 0.75f) + 1);
				for (int i = start; i < olen; i+=2) {
					object.put(ostack[i], ostack[i+1]);
				}
				olen = start;
				ostack = oexpand(ostack, olen + 1);
				ostack[olen++] = object;
				break;
			}
			
			btype = type;
		} while ((type = parser.next()) != null);
		
		return ostack[0];
	}
	
	public int getDepth() {
		return parser.getDepth();
	}
	
	private int[] iexpand(int[] array, int min) {
		if (min > array.length) {
			array = Arrays.copyOf(array, array.length * 3 / 2 + 1);
		}
		return array;
	}
	
	private Object[] oexpand(Object[] array, int min) {
		if (min > array.length) {
			array = Arrays.copyOf(array, array.length * 3 / 2 + 1);
		}
		return array;
	}
}
