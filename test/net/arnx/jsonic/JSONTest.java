/*
 * Copyright 2007-2009 Hidekatsu Izuno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package net.arnx.jsonic;

import static org.junit.Assert.*;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.*;

import javax.xml.parsers.DocumentBuilderFactory;

import net.arnx.jsonic.JSON;
import net.arnx.jsonic.JSON.Mode;
import net.arnx.jsonic.util.ClassUtil;
import net.arnx.jsonic.util.ExtendedDateFormat;

import org.junit.Test;
import org.seasar.framework.util.ReaderUtil;
import org.springframework.jdbc.core.CallableStatementCreatorFactory;
import org.w3c.dom.Document;

import org.apache.commons.beanutils.BasicDynaClass;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaClass;
import org.apache.commons.beanutils.DynaProperty;

@SuppressWarnings({"unchecked", "unused", "serial", "rawtypes"})
public class JSONTest {

	static class JSONTester implements Callable<Object> {
		@Override
		public Object call() throws Exception {
			JSONTest test = new JSONTest();
			test.testEncode();
			test.testDecodeString();
			test.testDecodeStringClassOfQextendsT();
			test.testFormat();
			test.testParse();
			return null;
		}
	}

	@Test
	public void testEncode() throws Exception {
		ArrayList<Object> list = new ArrayList<Object>();
		assertEquals("[]", JSON.encode(list));

		list.clear();
		list.add("\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u0008\u0009\n\u000B\u000C\r\u000E\u000F");

		assertEquals("[\"\\u0000\\u0001\\u0002\\u0003\\u0004\\u0005\\u0006\\u0007\\b\\t\\n\\u000B\\f\\r\\u000E\\u000F\"]", JSON.encode(list));

		list.clear();
		list.add("\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001A\u001B\u001C\u001D\u001E\u001F\u0020");
		assertEquals("[\"\\u0010\\u0011\\u0012\\u0013\\u0014\\u0015\\u0016\\u0017\\u0018\\u0019\\u001A\\u001B\\u001C\\u001D\\u001E\\u001F \"]", JSON.encode(list));

		list.clear();
		list.add("abc\u007F");
		list.add("abc\u2028");
		list.add("abc\u2029");
		assertEquals("[\"abc\\u007F\",\"abc\\u2028\",\"abc\\u2029\"]", JSON.encode(list));

		list.clear();
		list.add("");
		list.add(1);
		list.add(1.0);
		list.add('c');
		list.add(new char[]{'c', 'h', 'a', 'r', '[', ']'});
		list.add("string");
		list.add(true);
		list.add(false);
		list.add(null);
		list.add(new Object());
		list.add(new int[] {});
		list.add(Pattern.compile("\\.*"));
		list.add(boolean.class);
		list.add(ExampleEnum.Example0);
		list.add(ExampleExtendEnum.Example0);

		assertEquals("[\"\",1,1.0,\"c\",\"char[]\",\"string\",true,false,null,{},[],\"\\\\.*\",\"boolean\",\"Example0\",\"Example0\"]", JSON.encode(list));

		list.add(list);
		assertEquals("[\"\",1,1.0,\"c\",\"char[]\",\"string\",true,false,null,{},[],\"\\\\.*\",\"boolean\",\"Example0\",\"Example0\",null]", JSON.encode(list));

		assertEquals("[1,2,3]", JSON.encode(new short[] {1,2,3}));
		assertEquals("[1,2,3]", JSON.encode(new int[] {1,2,3}));
		assertEquals("[1,2,3]", JSON.encode(new long[] {1l,2l,3l}));
		assertEquals("[1,2,3]", JSON.encode(new Integer[] {1,2,3}));
		assertEquals("[1.0,2.0,3.0,\"NaN\",\"Infinity\",\"-Infinity\"]", JSON.encode(
				new float[] {1.0f,2.0f,3.0f,Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}));
		assertEquals("[1.0,2.0,3.0,\"NaN\",\"Infinity\",\"-Infinity\"]", JSON.encode(
				new double[] {1.0,2.0,3.0,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}));

		assertEquals("[\"ja\"]", JSON.encode(new Object[] {Locale.JAPANESE}));
		assertEquals("[\"ja-JP\"]", JSON.encode(new Object[] {Locale.JAPAN}));
		assertEquals("[\"ja-JP-osaka\"]", JSON.encode(new Object[] {new Locale("ja", "JP", "osaka")}));

		Date date = new Date();
		assertEquals("[" + date.getTime() + "]", JSON.encode(new Object[] {date}));

		Calendar cal = Calendar.getInstance();
		assertEquals("[" + cal.getTimeInMillis() + "]", JSON.encode(new Object[] {cal}));
		assertEquals("[" + cal.getTimeInMillis() + "," + cal.getTimeInMillis() + "," + cal.getTimeInMillis() + "]", JSON.encode(new Object[] {cal, cal, cal}));

		assertEquals("[\"NEW\",\"NEW\",\"NEW\"]", JSON.encode(new Object[] {Thread.State.NEW, Thread.State.NEW, Thread.State.NEW}));

		TimeZone timeZone = TimeZone.getDefault();
		assertEquals("[\"" + timeZone.getID() + "\",\"" + timeZone.getID() + "\"]", JSON.encode(new Object[] {timeZone, timeZone}));

		Charset charset = Charset.defaultCharset();
		assertEquals("[\"" + charset.name() + "\",\"" + charset.name() + "\"]", JSON.encode(new Object[] {charset, charset}));

		Map<String, Object> map = new HashMap<String, Object>();
		assertEquals("{}", JSON.encode(map));

		map.put("value", 1);
		assertEquals("{\"value\":1}", JSON.encode(map));

		Object obj = new Object();
		assertEquals("{}", JSON.encode(obj));

		obj = new Object() {
			public int getPublicValue() {
				return 1;
			}

			protected int getProtectedValue() {
				return 1;
			}
			int getFriendlyValue() {
				return 1;
			}
			private int getPrivateValue() {
				return 1;
			}
		};
		assertEquals("{\"publicValue\":1}", JSON.encode(obj));

		obj = new Object() {
			public int publicValue = 1;

			public transient int transientValue = 1;

			protected int protectedValue = 1;

			int friendlyValue = 1;

			private int privateValue = 1;
		};
		assertEquals("{\"publicValue\":1}", JSON.encode(obj));

		obj = new Object() {
			public int publicValue = 0;

			public int getPublicValue() {
				return 1;
			}

			public Object getMine() {
				return this;
			}
		};
		assertEquals("{\"publicValue\":1}", JSON.encode(obj));

		JavaBean bean = new JavaBean();
		bean.setaname("aname");
		bean.setbName("bName");
		bean.setCName("CName");
		assertEquals("{\"CName\":\"CName\",\"bName\":\"bName\"}", JSON.encode(bean));

		TestBean test = new TestBean();
		test.setA(100);
		test.e = Locale.ENGLISH;
		assertEquals("{\"_w\":100000000000000000000000,\"a\":100,\"b\":null,\"c\":false,\"class\":null,\"d\":null,\"e\":\"en\",\"f\":null,\"g\":null,\"h\":null,\"if\":null,\"漢字\":null}", JSON.encode(test));

		TestBeanWrapper tbw = new TestBeanWrapper();
		tbw.test = test;
		String result = JSON.encode(tbw);
		assertEquals("{\"_w\":100000000000000000000000,\"a\":100,\"b\":null,\"c\":false,\"class\":null,\"d\":null,\"e\":\"en\",\"f\":null,\"g\":null,\"h\":null,\"if\":null,\"漢字\":null}", JSON.encode(JSON.decode(result, TestBeanWrapper.class).test));

		Document doc = DocumentBuilderFactory
			.newInstance()
			.newDocumentBuilder()
			.parse(this.getClass().getResourceAsStream("Sample.xml"));
		String sample1 = ReaderUtil.readText(new InputStreamReader(this.getClass().getResourceAsStream("Sample1.json"), "UTF-8"));
		assertEquals(sample1, JSON.encode(doc));

		String sample2 = ReaderUtil.readText(new InputStreamReader(this.getClass().getResourceAsStream("Sample2.json"), "UTF-8")).replaceAll("\r\n", "\n");
		result = JSON.encode(doc, true);
		assertEquals(sample2, JSON.encode(doc, true));

		UUID uuid = UUID.randomUUID();

		list = new ArrayList<Object>();
		list.add(new URI("http://www.google.co.jp/"));
		list.add(new URL("http://www.google.co.jp/"));
		list.add(InetAddress.getByName("localhost"));
		list.add(Charset.forName("UTF-8"));
		list.add(uuid);
		assertEquals("[\"http://www.google.co.jp/\",\"http://www.google.co.jp/\",\"127.0.0.1\",\"UTF-8\",\"" + uuid + "\"]", JSON.encode(list));
		assertEquals("[\"http://www.google.co.jp/\",\"http://www.google.co.jp/\",\"127.0.0.1\",\"UTF-8\",\"" + uuid + "\"]", JSON.encode(list.iterator()));

		Vector v = new Vector(list);
		assertEquals("[\"http://www.google.co.jp/\",\"http://www.google.co.jp/\",\"127.0.0.1\",\"UTF-8\",\"" + uuid + "\"]", JSON.encode(v.elements()));

		list = new ArrayList<Object>();
		list.add(new File("./sample.txt"));
		String sep = (File.separatorChar == '\\') ? File.separator + File.separator : File.separator;
		assertEquals("[\"." + sep + "sample.txt\"]", JSON.encode(list));

		DynaClass dynaClass = new BasicDynaClass("TestDynaBean", null, new DynaProperty[] {
				new DynaProperty("a", int.class),
				new DynaProperty("b", String.class),
				new DynaProperty("c", boolean.class),
				new DynaProperty("d", Date.class),
		});
		DynaBean dynaBean = dynaClass.newInstance();
		dynaBean.set("a", 100);
		dynaBean.set("b", "string");
		dynaBean.set("c", true);
		assertEquals("{\"a\":100,\"b\":\"string\",\"c\":true,\"d\":null}", JSON.encode(dynaBean));

		AnnotationBean aBean = new AnnotationBean();
		aBean.field = 1;
		aBean.method = 2;
		aBean.dummy = 3;
		aBean.date = toDate(2009, 1, 1, 0, 0, 0, 0);
		aBean.array1 = new int[] {1, 2, 3};
		aBean.array2 = new Integer[] {1, 2, 3};
		aBean.json_data = "{\"a\": 100 /* ほげほげ */}";
		aBean.simple_json_data = "\"aaaa\"";
		aBean.number_json_data = 0.0;

		List<Integer> array3 = new ArrayList<Integer>();
		array3.add(1);
		array3.add(2);
		array3.add(3);
		aBean.array3 = array3;
		assertEquals(
				"{\"json_data\":{\"a\": 100 /* ほげほげ */},\"simple_json_data\":\"aaaa\",\"number_json_data\":0.0,"
				+ "\"a\":1,\"anonymMap\":null,\"array1\":[\"1.0\",\"2.0\",\"3.0\"],\"array2\":[\"1.0\",\"2.0\",\"3.0\"],"
				+ "\"array3\":[\"1.0\",\"2.0\",\"3.0\"],\"b\":\"002.0\",\"date\":\"2009/01/01\",\"method\":2,"
				+ "\"name_a\":\"aaa\",\"name_b\":\"aaa\",\"namex\":\"aaa\",\"namez\":\"aaa\"}", JSON.encode(aBean));

		obj = new Object() {
			@JSONHint(type=String.class)
			public int strnum = 1;

			@JSONHint(type=String.class)
			public Thread.State state = Thread.State.BLOCKED;
		};
		assertEquals("{\"state\":\"BLOCKED\",\"strnum\":\"1\"}", JSON.encode(obj));

		assertEquals("{\"list\":[\"test\"]}", JSON.encode(new ImplClass()));

		try {
			obj = new Object() {
				public int noException = 0;

				public Object getException() {
					throw new RuntimeException("エラー");
				}
			};
			JSON.encode(obj);
			fail();
		} catch (JSONException e) {
			assertNotNull(e);
		}

		assertEquals("[0,\"1\",2,\"3\",4,5,6,\"7\",\"8\"]", (new JSON() {
			@Override
			protected Object preformat(Context context, Object value) throws Exception {
				if ("0".equals(value) || "2".equals(value) || "4".equals(value)
						|| "5".equals(value) || "6".equals(value)) {
					return Integer.parseInt(value.toString());
				} else {
					return super.preformat(context, value);
				}
			}
		}).format(Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8")));
	}

	@Test
	public void testEscapeScript() throws Exception {
		assertEquals("null", JSON.escapeScript(null));
		assertEquals("100", JSON.escapeScript(100));
		assertEquals("100.1", JSON.escapeScript(100.1));
		assertEquals("\"string\"", JSON.escapeScript("string"));
		assertEquals("[1,2,3]", JSON.escapeScript(new short[] {1,2,3}));
		assertEquals("[1,2,3]", JSON.escapeScript(new int[] {1,2,3}));
		assertEquals("[1,2,3]", JSON.escapeScript(new long[] {1l,2l,3l}));
		assertEquals("[1.0,2.0,3.0,Number.NaN,Number.POSITIVE_INFINITY,Number.NEGATIVE_INFINITY]", JSON.escapeScript(
				new float[] {1.0f,2.0f,3.0f,Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}));
		assertEquals("[1.0,2.0,3.0,Number.NaN,Number.POSITIVE_INFINITY,Number.NEGATIVE_INFINITY]", JSON.escapeScript(
				new double[] {1.0,2.0,3.0,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}));

		Date date = new Date();
		assertEquals("new Date(" + date.getTime() + ")", JSON.escapeScript(date));
	}

	@Test
	public void testDecodeString() throws Exception {
		ArrayList<Object> list = new ArrayList<Object>();
		list.add(new HashMap());
		list.add(new ArrayList());
		list.add(new BigDecimal("1"));
		list.add("str'ing");
		list.add("");
		list.add(true);
		list.add(false);
		list.add(null);

		assertEquals(list, JSON.decode("[{}, [], 1, \"str'ing\", \"\", true, false, null]"));
		assertEquals(list, JSON.decode("\r[\t{\r}\n, [\t]\r,\n1 ,\t \r\"str'ing\"\n, \"\", true\t,\rfalse\n,\tnull\r]\n"));

		list.clear();
		list.add(new BigDecimal("0"));
		list.add(new BigDecimal("-0"));
		list.add(new BigDecimal("1"));
		list.add(new BigDecimal("-1"));
		list.add(new BigDecimal("999999999999999999"));
		list.add(new BigDecimal("-999999999999999999"));
		list.add(new BigDecimal("1000000000000000000"));
		list.add(new BigDecimal("-1000000000000000000"));
		list.add(new BigDecimal("9223372036854775807"));
		list.add(new BigDecimal("-9223372036854775807"));
		list.add(new BigDecimal("9223372036854775808"));
		list.add(new BigDecimal("-9223372036854775808"));

		assertEquals(list, JSON.decode("[0,-0,1,-1,999999999999999999,-999999999999999999,1000000000000000000,-1000000000000000000,9223372036854775807,-9223372036854775807,9223372036854775808,-9223372036854775808]"));

		list.clear();
		list.add(new BigDecimal("-1.1"));
		list.add(new BigDecimal("11.1"));
		list.add(new BigDecimal("11.1"));
		list.add(new BigDecimal("11.1"));
		list.add(new BigDecimal("1.11"));
		list.add(new BigDecimal("9999999999999.99999e+2"));
		list.add(new BigDecimal("-9999999999999.99999e+2"));
		list.add(new BigDecimal("100000000000000.0000e-2"));
		list.add(new BigDecimal("-100000000000000.0000e-2"));
		list.add(new BigDecimal("922337203685.4775807e2"));
		list.add(new BigDecimal("-922337203685.4775807e2"));
		list.add(new BigDecimal("92233720.36854775808e-2"));
		list.add(new BigDecimal("-92233720.36854775808e-2"));

		assertEquals(list, JSON.decode("[-1.1, 11.1e0, 1.11e1, 1.11E+1, 11.1e-1,"
				+ " 9999999999999.99999e+2, -9999999999999.99999e+2,"
				+ " 100000000000000.0000e-2, -100000000000000.0000e-2,"
				+ " 922337203685.4775807e2, -922337203685.4775807e2,"
				+ " 92233720.36854775808e-2, -92233720.36854775808e-2]"));

		list.clear();
		list.add(new BigDecimal("-1.1000000000"));
		list.add(new BigDecimal("11.1"));
		list.add(new BigDecimal("11.1"));
		list.add(new BigDecimal("1.11"));

		assertEquals(list, JSON.decode("[-11000000000e-10, 0.0000000000111E12, 11.1E+000, 11.1e-01]"));

		Map<String, Object> map1 = new LinkedHashMap<String, Object>();
		Map<String, Object> map2 = new LinkedHashMap<String, Object>();
		Map<String, Object> map3 = new LinkedHashMap<String, Object>();
		map1.put("map2", map2);
		map1.put("1", new BigDecimal("1"));
		map2.put("'2'", new BigDecimal("2"));
		map2.put("map3", map3);
		map3.put("'3", new BigDecimal("3"));

		assertEquals(map1, JSON.decode("{\"map2\": {\"'2'\": 2, \"map3\": {\"'3\": 3}}, \"1\": 1}"));

		Object[] input = new Object[2];
		input[0] = new Date();
		input[1] = Calendar.getInstance();

		List output = new ArrayList();
		output.add(new BigDecimal(((Date)input[0]).getTime()));
		output.add(new BigDecimal(((Calendar)input[1]).getTimeInMillis()));

		assertEquals(output, JSON.decode(JSON.encode(input)));

		try {
			JSON.decode("aaa: 1, bbb");
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		Map<String, Object> map4 = new LinkedHashMap<String, Object>();
		map4.put("aaa", new BigDecimal("1"));
		map4.put("bbb", null);

		assertEquals(map4, JSON.decode("aaa: 1, bbb: "));
		assertEquals(map4, JSON.decode("aaa: 1, bbb:\n "));

		assertEquals(JSON.decode("{\"sample1\":\"テスト1\",\"sample2\":\"テスト2\"}"),
				JSON.decode("{\"sample1\":\"\\u30c6\\u30b9\\u30c81\",\"sample2\":\"\\u30c6\\u30b9\\u30c82\"}"));

		StringBeanWrapper sbw = new StringBeanWrapper();
		sbw.sbean = new StringBean("string");
		sbw.state = Thread.State.BLOCKED;
		sbw.text = "string";

		assertEquals(sbw, JSON.decode("{\"state\":\"BLOCKED\",\"sbean\":\"string\",\"text\":\"string\"}", StringBeanWrapper.class));

		GenericPropertyTestWrapper gptw = new GenericPropertyTestWrapper();
		gptw.test.property = "test";

		assertEquals(gptw, JSON.decode("{\"test\":{\"property\":\"test\"}}", GenericPropertyTestWrapper.class));

		InputStream in = this.getClass().getResourceAsStream("LongString.json");
		ArrayList list2 = new ArrayList();
		list2.add(repeat("a", 1000));
		assertEquals(list2, JSON.decode(in));
		in.close();
	}

	private static String repeat(String text, int count) {
		StringBuilder sb = new StringBuilder(text.length() * count);
		for (int i = 0; i < count; i++) {
			sb.append(text);
		}
		return sb.toString();
	}

	public static class HogeList extends ArrayList<MyData> {

	}

	public static class MyData {
		public String data1;
		public String data2;
		@Override
		public String toString() {
			return "MyData [data1=" + data1 + ", data2=" + data2 + "]";
		}
	}

	@Test
	public void testDecodeStringClassOfQextendsT() throws Exception {
		ArrayList<Object> list = new ArrayList<Object>();
		list.add(new HashMap());
		list.add(new ArrayList());
		list.add(new BigDecimal("1"));
		list.add("string");
		list.add(true);
		list.add(false);
		list.add(null);

		assertEquals(list, JSON.decode(JSON.encode(list), List.class));

		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put("test", "result");

		assertEquals(map, JSON.decode(JSON.encode(map), TreeMap.class));

		JavaBean bean = new JavaBean();
		bean.setaname("aname");
		bean.setbName("bName");
		bean.setCName("CName");

		JavaBean beanResult = new JavaBean();
		beanResult.setbName("bName");
		beanResult.setCName("CName");

		assertEquals(beanResult, JSON.decode(JSON.encode(bean), bean.getClass()));

		TestBean test = new TestBean();
		test.setA(100);
		test.b = "hoge-hoge";
		test.setC(false);
		test.d = new Date();
		test.e = Locale.JAPAN;
		test.setG(Pattern.compile("\\.*"));
		test.setH(TimeZone.getTimeZone("JST"));
		test.class_ = boolean.class;

		String json = JSON.encode(test);
		TestBean result = JSON.decode(json, TestBean.class);

		assertEquals(test, result);

		test = new TestBean();
		test.setA(0);
		test.b = "hoge-hoge";
		test.setC(false);
		test.d = null;
		test.e = Locale.JAPAN;
		test.setG(Pattern.compile("\\.*"));
		test.setH(TimeZone.getTimeZone("Asia/Tokyo"));
		test.class_ = Object.class;

		assertEquals(test, JSON.decode("{\"a\":null,\"b\":\"hoge-hoge\",\"c\":false,\"class\":\"java.lang.Object\",\"d\":null,\"e\":[\"ja\", \"JP\"],\"g\":\"\\\\.*\",\"h\":\"Asia/Tokyo\"}", TestBean.class));

		GenericsBean gb = new GenericsBean();
		List<String> list2 = new ArrayList<String>();
		list2.add("1");
		list2.add("false");
		gb.setList(list2);

		InheritList ilist = new InheritList();
		ilist.add("1");
		ilist.add("false");
		gb.ilist = ilist;

		InheritList2 ilist2 = new InheritList2();
		ilist2.add("1");
		ilist2.add("false");
		gb.ilist2 = ilist2;

		InheritMap imap = new InheritMap();
		imap.put(1, "1");
		imap.put(2, "false");
		gb.imap = imap;

		InheritMap2 imap2 = new InheritMap2();
		imap2.put(1, "1");
		imap2.put(2, "false");
		gb.imap2 = imap2;

		Map<String, String> gmap = new HashMap<String, String>();
		gmap.put("1", "1");
		gmap.put("true", "true");
		gb.setMap(gmap);

		Map<String, Integer> map2 = new HashMap<String, Integer>();
		map2.put("0", 0);
		map2.put("1", 1);
		gb.map2 = map2;

		Map<Integer, String> map3 = new HashMap<Integer, String>();
		map3.put(0, "false");
		map3.put(1, "true");
		gb.map3 = map3;

		List<List<String>> glist2 = new ArrayList<List<String>>();
		glist2.add(new ArrayList<String>() {
			{
				add("1");
				add("false");
			}
		});
		gb.setGenericsList(glist2);

		GenericsBean out = JSON.decode("{\"ilist\": [1, false],\"ilist2\": [1, false],\"imap\": {'1':1, '2':false},\"imap2\": {'1':1, '2':false},\"list\": [1, false], \"map\": {\"1\": 1, \"true\": true}, \"genericsList\": [[1, false]], \"map2\": [false, true], \"map3\": {\"0\": false, \"1\": true}}", GenericsBean.class);
		assertEquals(gb, out);

		AnnotationBean aBean = new AnnotationBean();
		aBean.field = 1;
		aBean.method = 2;
		aBean.dummy = 0;
		aBean.date = toDate(2009, 1, 1, 0, 0, 0, 0);
		aBean.array1 = new int[] {1, 2, 3};
		aBean.array2 = new Integer[] {1, 2, 3};
		aBean.json_data = "{\"a\":[100,null,\"aaa\",{\"key\":\"value\"}]}";
		aBean.simple_json_data = "\"aaaa\"";
		aBean.number_json_data = 0.0;

		List<Integer> array3 = new Vector<Integer>();
		array3.add(1);
		array3.add(2);
		array3.add(3);
		aBean.array3 = array3;

		AnonymTest anonymMap = new AnonymTest();
		anonymMap.anonym = "test";
		aBean.anonymMap = anonymMap;

		AnnotationBean aBeanResult = JSON.decode("{\"a\":1,\"anonymMap\":\"test\","
				+ "\"array1\":[\"1.0\",\"2.0\",\"3.0\"],\"array2\":[\"1.0\",\"2.0\",\"3.0\"],\"array3\":[\"1.0\",\"2.0\",\"3.0\"],"
				+ "\"b\":\"2.01\",\"date\":\"2009/01/01\","
				+ "json_data: {\"a\": [100 /* ほげほげ */, \nnull,'aaa',{key : \"value\"}]  }, \"simple_json_data\": 'aaaa', \"number_json_data\": 0,"
				+ "\"method\":2}", AnnotationBean.class);
		aBean.hashCode();
		aBeanResult.hashCode();
		assertEquals(aBean, aBeanResult);

		TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
		Class cls = cl.loadClass("net.arnx.jsonic.TestClassLoader$TestBean");

		assertEquals(cls.newInstance(), JSON.decode("{\"class\": {\"classloader\": {\"vulnerability\": true}}}", cls));
	}

	@Test
	public void testFormat() throws Exception {
		JSON json = new JSON();
		ArrayList<Object> list = new ArrayList<Object>();
		assertEquals("[]", json.format(list, new StringWriter()).toString());

		list.add(1);
		list.add(1.0);
		list.add('c');
		list.add(new char[]{'c', 'h', 'a', 'r', '[', ']'});
		list.add("string");
		list.add(true);
		list.add(false);
		list.add(null);
		list.add(new TreeMap() {
			private static final long serialVersionUID = 1L;

			{
				put("a", "a");
				put("b", new int[] {1,2,3,4,5});
				put("c", new TreeMap() {
					private static final long serialVersionUID = 1L;
					{
						put("a", "a");
					}
				});
			}
		});
		list.add(new int[] {1,2,3,4,5});

		json.setPrettyPrint(true);
		assertEquals("[\n\t1,\n\t1.0,\n\t\"c\",\n\t\"char[]\",\n\t\"string\",\n\ttrue,\n\tfalse,\n\tnull,"
				+ "\n\t{\n\t\t\"a\": \"a\",\n\t\t\"b\": [1, 2, 3, 4, 5],\n\t\t\"c\": {\n\t\t\t\"a\": \"a\"\n\t\t}\n\t},\n\t[1, 2, 3, 4, 5]\n]",
				json.format(list, new StringWriter()).toString());

		json.setIndentText(" ");
		assertEquals("[\n 1,\n 1.0,\n \"c\",\n \"char[]\",\n \"string\",\n true,\n false,\n null,"
				+ "\n {\n  \"a\": \"a\",\n  \"b\": [1, 2, 3, 4, 5],\n  \"c\": {\n   \"a\": \"a\"\n  }\n },\n [1, 2, 3, 4, 5]\n]",
				json.format(list, new StringBuilder()).toString());

		json.setInitialIndent(3);
		assertEquals("   [\n    1,\n    1.0,\n    \"c\",\n    \"char[]\",\n    \"string\",\n    true,\n    false,\n    null,"
				+ "\n    {\n     \"a\": \"a\",\n     \"b\": [1, 2, 3, 4, 5],\n     \"c\": {\n      \"a\": \"a\"\n     }\n    },\n    [1, 2, 3, 4, 5]\n   ]",
				json.format(list, new StringWriter()).toString());

		json.setPrettyPrint(false);
		assertEquals("true", json.format(true, new StringBuilder()).toString());

		assertEquals("\"net.arnx.jsonic.JSON\"", json.format(JSON.class, new StringBuilder()).toString());

		assertEquals("\"ja-JP\"", json.format(Locale.JAPAN, new StringBuilder()).toString());

		assertEquals("[\"NaN\",\"Infinity\",\"-Infinity\"]", json.format(
				new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}, new StringWriter()).toString());

		Date d = new Date();
		assertEquals("[" + Long.toString(d.getTime()) + "]", json.format(new Date[] {d}, new StringBuilder()).toString());


		assertEquals("[\"AQID\"]", json.format(new byte[][] {{1,2,3}}, new StringWriter()).toString());

		Object obj = new Object() {
			public Object a = 100;
			public Object b = null;
			public List list = new ArrayList() {
				{
					add(100);
					add(null);
				}
			};
		};
		json.setSuppressNull(true);
		assertEquals("{\"a\":100,\"list\":[100,null]}", json.format(obj));

		json.setPrettyPrint(true);
		json.setIndentText("\t");
		json.setInitialIndent(0);
		assertEquals("{\n\t\"a\": 100,\n\t\"list\": [\n\t\t100,\n\t\tnull\n\t]\n}", json.format(obj));

		json = new Point2DJSON();
		assertEquals("[10.5,10.5]", json.format(new Point2D.Double(10.5, 10.5)));
		assertEquals("[10.5,10.5]", json.format(new Point2D.Float(10.5f, 10.5f)));
		assertEquals("[10.0,10.0]", json.format(new Point(10, 10)));

		assertEquals("[\"!\\\"#$%&'()=~|\\u003C\\u003E?_\"]", json.format(new String[] { "!\"#$%&'()=~|<>?_" }));

		json = new JSON();

		NamedBean2 nb2 = new NamedBean2();
		nb2.aaaBbb = 2;
		nb2.bbbAaa = 0;
		assertEquals("{\"aaaBbb\":2,\"bbbAaa\":2}", json.format(nb2));

		Object obj2 = new Object() {
			public int abcDef = 100;
			public int GhiJkl = 100;
			public int mno_pqr = 100;
			public int STU_VWX = 100;
		};
		assertEquals("{\"GhiJkl\":100,\"STU_VWX\":100,\"abcDef\":100,\"mno_pqr\":100}", json.format(obj2));
		json.setPropertyStyle(NamingStyle.LOWER_CASE);
		assertEquals("{\"abcdef\":100,\"ghijkl\":100,\"mno_pqr\":100,\"stu_vwx\":100}", json.format(obj2));
		json.setPropertyStyle(NamingStyle.LOWER_CAMEL);
		assertEquals("{\"abcDef\":100,\"ghiJkl\":100,\"mnoPqr\":100,\"stuVwx\":100}", json.format(obj2));
		json.setPropertyStyle(NamingStyle.LOWER_UNDERSCORE);
		assertEquals("{\"abc_def\":100,\"ghi_jkl\":100,\"mno_pqr\":100,\"stu_vwx\":100}", json.format(obj2));

		//SCRIPT
		json = new JSON();
		json.setMode(JSON.Mode.SCRIPT);

		assertEquals("null", json.format(null, new StringBuilder()).toString());
		assertEquals("1000", json.format(1000, new StringBuilder()).toString());
		assertEquals("\"test\"", json.format("test", new StringBuilder()).toString());
		assertEquals("[Number.NaN,Number.POSITIVE_INFINITY,Number.NEGATIVE_INFINITY]", json.format(
				new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}, new StringBuilder()).toString());

		assertEquals("new Date(" + Long.toString(d.getTime()) + ")", json.format(d, new StringBuilder()).toString());
		assertEquals("[\"!\\\"#$%&'()=~|\\u003C\\u003E?_\"]", json.format(new String[] { "!\"#$%&'()=~|<>?_" }));
		assertArrayEquals(new String[] { "!\"#$%&'()=~|<>?_" }, json.parse("[\"!\\\"#$%&'()=~|\\u003C\\u003E?_\"]", String[].class));

		//STRICT
		json = new JSON();
		json.setMode(Mode.STRICT);

		assertEquals("[\"!\\\"#$%&'()=~|<>?_\"]", json.format(new String[] { "!\"#$%&'()=~|<>?_" }));

		assertEquals("null", json.format(null, new StringBuilder()).toString());

		assertEquals("1000", json.format(1000, new StringBuilder()).toString());

		assertEquals("\"test\"", json.format("test", new StringBuilder()).toString());

		DateNumberTestClass dates = new DateNumberTestClass();
		dates.a = toDate(2000, 1, 1, 12, 5, 6, 0);
		dates.b = toDate(2001, 1, 1, 12, 5, 6, 1);
		dates.c = 1000;
		dates.d = 1001;

		json = new JSON();
		json.setDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
		json.setNumberFormat("000,000.00");
		assertEquals("{\"a\":\"2000/01/01 12:05:06.000\",\"b\":\"2001-01\",\"c\":\"001,000.00\",\"d\":\"1001.000\"}", json.format(dates));

		json = new JSON();
		json.setDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
		assertEquals("{\"a\":\"2000/01/01 12:05:06.000\",\"b\":\"2001-01\",\"c\":1000,\"d\":\"1001.000\"}", json.format(dates));

		json = new JSON();
		json.setNumberFormat("000,000.00");
		assertEquals("{\"a\":" + dates.a.getTime() + ",\"b\":\"2001-01\",\"c\":\"001,000.00\",\"d\":\"1001.000\"}", json.format(dates));

		json.setDateFormat(null);
		json.setNumberFormat(null);
		assertEquals("{\"a\":" + dates.a.getTime() + ",\"b\":\"2001-01\",\"c\":1000,\"d\":\"1001.000\"}", json.format(dates));

		NamedTestClass named = new NamedTestClass();
		named.aaaAaaAaa = NamedTestEnum.aaaAaaAaa;
		named.AAA_BBB_CCC = NamedTestEnum.AAA_BBB_CCC;
		named.aaaあああ = NamedTestEnum.aaaあああ;

		json = new JSON() {
			@Override
			protected String normalize(String name) {
				if (name.equals("aaaAaaAaa")) {
					return "ABC_DEF";
				}
				return super.normalize(name);
			}
		};
		assertEquals("{\"AAA_BBB_CCC\":\"AAA_BBB_CCC\",\"ABC_DEF\":\"aaaAaaAaa\",\"aaaあああ\":\"aaaあああ\"}", json.format(named));

		json.setPropertyStyle(NamingStyle.LOWER_CAMEL);
		assertEquals("{\"aaaBbbCcc\":\"AAA_BBB_CCC\",\"aaaあああ\":\"aaaあああ\",\"abcDef\":\"aaaAaaAaa\"}", json.format(named));

		json = new JSON();

		json.setPropertyStyle(NamingStyle.NOOP);
		json.setEnumStyle(null);
		assertEquals("{\"AAA_BBB_CCC\":1,\"aaaAaaAaa\":0,\"aaaあああ\":2}", json.format(named));

		json.setPropertyStyle(NamingStyle.NOOP);
		json.setEnumStyle(NamingStyle.NOOP);
		assertEquals("{\"AAA_BBB_CCC\":\"AAA_BBB_CCC\",\"aaaAaaAaa\":\"aaaAaaAaa\",\"aaaあああ\":\"aaaあああ\"}", json.format(named));

		json.setPropertyStyle(NamingStyle.LOWER_CASE);
		json.setEnumStyle(NamingStyle.UPPER_CASE);
		assertEquals("{\"aaa_bbb_ccc\":\"AAA_BBB_CCC\",\"aaaaaaaaa\":\"AAAAAAAAA\",\"aaaあああ\":\"AAAあああ\"}", json.format(named));

		json.setPropertyStyle(NamingStyle.LOWER_CAMEL);
		json.setEnumStyle(NamingStyle.UPPER_CAMEL);
		assertEquals("{\"aaaAaaAaa\":\"AaaAaaAaa\",\"aaaBbbCcc\":\"AaaBbbCcc\",\"aaaあああ\":\"Aaaあああ\"}", json.format(named));

		json.setPropertyStyle(NamingStyle.LOWER_UNDERSCORE);
		json.setEnumStyle(NamingStyle.UPPER_UNDERSCORE);
		assertEquals("{\"aaa_aaa_aaa\":\"AAA_AAA_AAA\",\"aaa_bbb_ccc\":\"AAA_BBB_CCC\",\"aaaあああ\":\"AAAあああ\"}", json.format(named));

		json.setPropertyStyle(NamingStyle.UPPER_CASE);
		json.setEnumStyle(NamingStyle.LOWER_CASE);
		assertEquals("{\"AAAAAAAAA\":\"aaaaaaaaa\",\"AAA_BBB_CCC\":\"aaa_bbb_ccc\",\"AAAあああ\":\"aaaあああ\"}", json.format(named));

		json.setPropertyStyle(NamingStyle.UPPER_CAMEL);
		json.setEnumStyle(NamingStyle.LOWER_CAMEL);
		assertEquals("{\"AaaAaaAaa\":\"aaaAaaAaa\",\"AaaBbbCcc\":\"aaaBbbCcc\",\"Aaaあああ\":\"aaaあああ\"}", json.format(named));

		json.setPropertyStyle(NamingStyle.UPPER_UNDERSCORE);
		json.setEnumStyle(NamingStyle.LOWER_UNDERSCORE);
		assertEquals("{\"AAA_AAA_AAA\":\"aaa_aaa_aaa\",\"AAA_BBB_CCC\":\"aaa_bbb_ccc\",\"AAAあああ\":\"aaaあああ\"}", json.format(named));

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		json.format(Arrays.asList(1, "abc", true, 2.0), bout);
        assertEquals("[1,\"abc\",true,2.0]", bout.toString("UTF-8"));
	}

	@Test
	public void testParse() throws Exception {
		Locale.setDefault(Locale.JAPANESE);
		JSON json = new JSON();

		assertNull(json.parse("null"));
		assertEquals(Boolean.TRUE, json.parse("true"));
		assertEquals(Boolean.FALSE, json.parse("false"));
		assertEquals("", json.parse("\"\""));
		assertEquals("test", json.parse("\"test\""));
		assertEquals(new BigDecimal(10), json.parse("10"));
		assertEquals(new BigDecimal(-10), json.parse("-10"));
		assertEquals(new BigDecimal("10.1"), json.parse("10.1"));

		try {
			CharSequence cs = null;
			assertEquals(null, json.parse(cs));
			fail();
		} catch (NullPointerException e) {
			System.out.println(e);
			assertNotNull(e);
		}

		try {
			Reader reader = null;
			assertEquals(null, json.parse(reader));
			fail();
		} catch (NullPointerException e) {
			System.out.println(e);
			assertNotNull(e);
		}

		assertEquals(new LinkedHashMap(), json.parse(""));

		ArrayList<Object> list = new ArrayList<Object>();
		list.add(new HashMap() {
			{
				put("maa", "bbb");
			}
		});
		list.add(new ArrayList());
		list.add(new BigDecimal("1"));
		list.add("str'ing");
		list.add(true);
		list.add(false);
		list.add(null);

		try {
			assertEquals(list, json.parse("[{\"maa\": \"bbb\"}, [], 1, \"str\\'ing\", true, false, null"));
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		json.parse("[{'maa': 'bbb'}, [], 1, 'str\\'ing', true, false, null]");

		assertEquals(list, json.parse("[{\\u006daa: \"bbb\"}, [], 1, \"str'ing\", true, false, null]"));

		assertEquals(list, json.parse("[{\"\\u006daa\": \"bbb\"}, [/**/], 1, \"str'ing\", true, false, null]"));

		try {
			assertEquals(list, json.parse("[{'\\u006daa\": 'bbb'}, [], 1, \"str'ing\", true, false, null]"));
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		try {
			assertEquals(list, json.parse("[{\"maa': 'bbb'}, [], 1, \"str'ing\", true, false, null]"));
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		assertEquals(list, json.parse("[{   \t\\u006da\\u0061   : 'bbb'}, [], 1, \"str'ing\", true, false, null]"));

		list.set(0, new HashMap() {
			{
				put("float", "bbb");
			}
		});

		assertEquals(list, json.parse("[{float   : 'bbb'}, [], 1, \"str'ing\", true, false, null]"));

		list.set(0, new HashMap() {
			{
				put("0float", "bbb");
			}
		});

		try {
			assertEquals(list, json.parse("[{0float   : 'bbb'}, [], 1, \"str'ing\", true, false, null]"));
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		list.set(0, new HashMap() {
			{
				put("\\u006daa\\", "bbb");
			}
		});
		assertEquals(list, json.parse("[{'\\\\u006daa\\\\': 'bbb'}, [], 1, \"str'ing\", true, false, null]"));

		list.set(0, new HashMap() {
			{
				put("\\u006Daa", "bbb");
			}
		});
		assertEquals(list, json.parse("[{'\\\\u006Daa': 'bbb'}, [], 1, \"str'ing\", true, false, null]"));

		list.set(0, new HashMap() {
			{
				put("float0", "bbb");
			}
		});
		assertEquals(list, json.parse("[{float0   : 'bbb'}, [], 1, \"str'ing\", true, false, null]"));

		assertEquals(new HashMap() {{put("true", true);}}, json.parse("  true: true  "));
		assertEquals(new HashMap() {{put("false", false);}}, json.parse("  false: false  "));
		assertEquals(new HashMap() {{put("100", new BigDecimal("100"));}}, json.parse("  100: 100  "));
		assertEquals(new HashMap() {{put(null, null);}}, json.parse("  null: null  "));
		assertEquals(new HashMap() {{put("number", new BigDecimal(-100));}}, json.parse(" number: -100  "));

		try {
			json.parse("  {true: true  ");
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		try {
			json.parse(" number: -100  }");
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		assertEquals(new HashMap() {
			{
				put("numbers", new HashMap() {
					{
						put("number", new BigDecimal(-100));
					}
				});
			}
		}, json.parse(" numbers: { number: -100 } "));

		try {
			assertEquals(new HashMap() {
				{
					put("numbers", new HashMap() {
						{
							put("number", new BigDecimal(-100));
						}
					});
				}
			}, json.parse(" numbers: { number: -100 "));
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		assertEquals(list, json.parse("/*\n x\r */[/* x */{float0 //b\n  :/***/ 'bbb'}//d\r\r\r\r,//d\r\r\r\r"
				+ " [/*#\n x\r */], 1, \"str\\'in\\g\",/*\n x\r */ true/*\n x\r */, false, null/*\n x\r */] /*\n x\r */ //  aaaa"));

		NamedBean nb = new NamedBean();
		nb.namedPropertyAaa = 100;
		assertEquals(nb, json.parse("{\"namedPropertyAaa\":100}", NamedBean.class));
		assertEquals(nb, json.parse("{\"named property aaa\":100}", NamedBean.class));
		assertEquals(nb, json.parse("{\"named_property_aaa\":100}", NamedBean.class));
		assertEquals(nb, json.parse("{\"Named Property Aaa\":100}", NamedBean.class));

		NamedBean2 nb2 = new NamedBean2();
		nb2.aaaBbb = 2;
		nb2.bbbAaa = 0;
		assertEquals(nb2, json.parse("{\"bbbAaa\":2}", NamedBean2.class));

		Map map1 = new LinkedHashMap() {
			{
				put("map", new LinkedHashMap() {
					{
						put("string", "string_aaa");
						put("int", new BigDecimal(100));
					}
				});
				put("list", new ArrayList() {
					{
						add("string");
						add(new BigDecimal(100));
					}
				});
			}
		};
		assertEquals(map1, json.parse("map: {string: string_aaa  \t \nint:100}\n list:[ string, 100]"));
		assertEquals(map1, json.parse("map {string: string_aaa  \t \nint:100}\n list:[ string\n 100]"));
		assertEquals(map1, json.parse("\"map\" {string: string_aaa  \t \nint:100}\n list:[ string\n 100]"));
		assertEquals(map1, json.parse("'map' {string: string_aaa  \t \nint:100}\n list:[ string\n 100]"));

		Map map2 = new LinkedHashMap() {
			{
				put("emap", new LinkedHashMap());
				put("map", new LinkedHashMap() {
					{
						put("string", null);
						put("int", null);
					}
				});
				put("elist", new ArrayList());
				put("list", new ArrayList() {
					{
						add(null);
						add("string");
						add(null);
					}
				});
			}
		};

		Map map4 = new LinkedHashMap();

		json.setMaxDepth(1);
		assertEquals(map4, json.parse("{'1': '1'}"));

		json.setMaxDepth(2);
		map4.put("1", "1");
		assertEquals(map4, json.parse("{'1': '1'}"));

		List map4_2 = new ArrayList();
		map4.put("2", map4_2);
		assertEquals(map4, json.parse("{'1': '1', '2': ['2']}"));

		json.setMaxDepth(3);
		map4_2.add("2");
		assertEquals(map4, json.parse("{'1': '1', '2': ['2']}"));

		Map map4_3 = new LinkedHashMap();
		List map4_3_1 = new ArrayList();
		map4_3.put("3_1", map4_3_1);
		map4.put("3", map4_3);
		assertEquals(map4, json.parse("{'1': '1', '2': ['2'], '3': {'3_1': ['3']}}"));

		json.setMaxDepth(4);
		map4_3_1.add("3");
		assertEquals(map4, json.parse("{'1': '1', '2': ['2'], '3': {'3_1': ['3']}}"));

		json.setMaxDepth(32);
		assertEquals(map2, json.parse("emap:{}, map: {string: , int:}, elist:[],list: [,string, ]"));

		Map map3 = new LinkedHashMap() {
			{
				put("database", new LinkedHashMap() {
					{
						put("description", "ms sql server\n\tconnecter settings");
						put("user", "sa");
						put("password", "xxxx");
					}
				});
			}
		};
		assertEquals(map3, json.parse("// database settings\ndatabase {\n  description: 'ms sql server\n\tconnecter settings'\n  user: sa\n  password:"
				+ " xxxx // you need to replace your password.\n}\n/* {\"database\": {\"description\": \"ms sql server\", \"user\": \"sa\", \"password\": \"xxxx\"}} */\n"));

		InheritedBean ibean = new InheritedBean();
		ibean.map0 = new LinkedHashMap();
		ibean.map0.put("10", new BigDecimal("10"));
		ibean.map1 = new LinkedHashMap();
		ibean.map1.put("11", new BigDecimal("11"));
		ibean.map2 = new SuperLinkedHashMap();
		ibean.map2.put("12", new BigDecimal("12"));
		ibean.list0 = new ArrayList();
		ibean.list0.add(new BigDecimal("13"));
		ibean.list1 = new ArrayList();
		ibean.list1.add(new BigDecimal("14"));
		ibean.list2 = new SuperArrayList();
		ibean.list2.add(new BigDecimal("15"));
		assertEquals(ibean, json.parse("{map0:{'10':10},map1:{'11':11},map2:{'12':12},list0:[13],list1:[14],list2:[15]}", InheritedBean.class));

		List list2 = new ArrayList();
		list2.add("あいうえお");

		assertEquals(list2, json.parse(this.getClass().getResourceAsStream("UTF-8.json")));
		assertEquals(list2, json.parse(this.getClass().getResourceAsStream("UTF-16BE.json")));
		assertEquals(list2, json.parse(this.getClass().getResourceAsStream("UTF-16LE.json")));

		try {
			assertEquals(list2, json.parse(this.getClass().getResourceAsStream("UTF-32BE.json")));
			assertEquals(list2, json.parse(this.getClass().getResourceAsStream("UTF-32LE.json")));
		} catch (UnsupportedEncodingException e) {
			// skip
		}

		assertEquals(list2, json.parse(this.getClass().getResourceAsStream("UTF-8_BOM.json")));
		assertEquals(list2, json.parse(this.getClass().getResourceAsStream("UTF-16BE_BOM.json")));
		assertEquals(list2, json.parse(this.getClass().getResourceAsStream("UTF-16LE_BOM.json")));
		try {
			assertEquals(list2, json.parse(this.getClass().getResourceAsStream("UTF-32BE_BOM.json")));
			assertEquals(list2, json.parse(this.getClass().getResourceAsStream("UTF-32LE_BOM.json")));
		} catch (UnsupportedEncodingException e) {
			// skip
		}

		SuppressNullBean snb = new SuppressNullBean();
		snb.a = null;
		snb.b = null;
		snb.list = null;
		json.setSuppressNull(true);
		assertEquals(snb, json.parse("{\"a\":null,\"b\":null,\"list\":null}", SuppressNullBean.class));
		assertEquals(snb, json.parse("{\"a\":null,\"b\":,\"list\":}", SuppressNullBean.class));
		json.setSuppressNull(false);
		assertEquals(snb, json.parse("{\"a\":null,\"b\":null,\"list\":null}", SuppressNullBean.class));
		assertEquals(snb, json.parse("{\"a\":null,\"b\":,\"list\":}", SuppressNullBean.class));

		json = new Point2DJSON();
		assertEquals(new Point2D.Double(10.5, 10.5), json.parse("[10.5,10.5]", Point2D.class));

		json = new JSON();

		Map<Object, Object> ssMap = new LinkedHashMap<Object, Object>();
		ssMap.put("aaa", new BigDecimal("1"));
		ssMap.put("bbb", new BigDecimal("2"));
		ssMap.put("ccc.bbb", new BigDecimal("3"));
		ssMap.put("ccc.ddd.0", new BigDecimal("4"));
		ssMap.put("ccc.ddd.1", new BigDecimal("5"));
		ssMap.put("ccc.ddd.2", new BigDecimal("6"));
		ssMap.put("eee", false);
		ssMap.put("fff.0.ggg", "x");
		ssMap.put("fff.0.hhh", "y");
		ssMap.put("fff.1.ggg", "z");
		ssMap.put("fff.1.hhh", "0");

		Properties props = new Properties();
		for (Map.Entry<Object, Object> entry : ssMap.entrySet()) {
			props.setProperty(entry.getKey().toString(), entry.getValue().toString());
		}

		assertEquals(props, json.parse("{aaa:1,bbb:2,ccc:{bbb:3,ddd:[4,5,6]},eee:false,"
				+"fff:[{ggg:\"x\", hhh:\"y\"}, {ggg:\"z\", hhh:\"0\"}]}",
				Properties.class));

		props.clear();
		props.setProperty("0", "aaa");
		props.setProperty("1.bbb", "1");
		props.setProperty("1.ccc", "2");
		props.setProperty("2", "false");

		assertEquals(props, json.parse("[\"aaa\",{bbb:1,ccc:2},false]", Properties.class));

		List<TestBean> list5 = JSON.decode("[ {} ]",  (new ArrayList<TestBean>() {}).getClass().getGenericSuperclass());
		assertEquals(TestBean.class, list5.get(0).getClass());

		List<TestBean> list6 = JSON.decode("[ {} ]",  new TypeReference<List<TestBean>>() {});
		assertEquals(TestBean.class, list5.get(0).getClass());

		//SCRIPT
		json.setMode(JSON.Mode.SCRIPT);

		assertNull(json.parse("null"));
		assertEquals(Boolean.TRUE, json.parse("true"));
		assertEquals(Boolean.FALSE, json.parse("false"));
		assertEquals("", json.parse("\"\""));
		assertEquals("test", json.parse("\"test\""));
		assertEquals(new BigDecimal(10), json.parse("10"));
		assertEquals(new BigDecimal(-10), json.parse("-10"));
		assertEquals(new BigDecimal("10.1"), json.parse("10.1"));


		assertEquals(new HashMap() {{put("true", true);}}, json.parse("{  true: true  }"));
		assertEquals(new HashMap() {{put("false", false);}}, json.parse("{  false: false  }"));
		assertEquals(new HashMap() {{put("100", new BigDecimal("100"));}}, json.parse("{  100: 100  }"));
		assertEquals(new HashMap() {{put("null", null);}}, json.parse("{  null: null  }"));
		assertEquals(new HashMap() {{put("number", new BigDecimal(-100));}}, json.parse("{ number: -100  }"));

		try {
			json.parse("{-100: 100}");
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		try {
			json.parse("");
			fail();
		} catch (JSONException e) {
			System.out.println(e);
			assertNotNull(e);
		}

		assertEquals(JSON.decode("[]"), json.parse("[\n]"));

		try {
			json.parse("[100\n200]");
			fail();
		} catch (JSONException e) {
			System.out.println(e);
			assertNotNull(e);
		}

		try {
			json.parse("[100,]");
			fail();
		} catch (JSONException e) {
			System.out.println(e);
			assertNotNull(e);
		}

		try {
			json.parse("{\"aaa\":}");
			fail();
		} catch (JSONException e) {
			System.out.println(e);
			assertNotNull(e);
		}

		try {
			json.parse("{\"aaa\":,\"bbb\":\"ccc\"}");
			fail();
		} catch (JSONException e) {
			System.out.println(e);
			assertNotNull(e);
		}

		json.parse("{} /**/");

		try {
			json.parse("{} #aaa");
			fail();
		} catch (JSONException e) {
			System.out.println(e);
			assertNotNull(e);
		}

		//STRICT
		json.setMode(JSON.Mode.STRICT);

		try {
			json.parse("");
			fail();
		} catch (JSONException e) {
			System.out.println(e);
			assertNotNull(e);
		}

		assertNull(json.parse("null"));
		assertEquals(Boolean.TRUE, json.parse("true"));
		assertEquals(Boolean.FALSE, json.parse("false"));
		assertEquals("", json.parse("\"\""));
		assertEquals("test", json.parse("\"test\""));
		assertEquals(new BigDecimal(10), json.parse("10"));
		assertEquals(new BigDecimal(-10), json.parse("-10"));
		assertEquals(new BigDecimal("10.1"), json.parse("10.1"));

		assertEquals(JSON.decode("[]"), json.parse("[\n]"));
		assertEquals(JSON.decode("[\" '\"]"), json.parse("[\n\" '\"]"));

		try {
			json.parse("[100\n200]");
			fail();
		} catch (JSONException e) {
			System.out.println(e);
			assertNotNull(e);
		}

		try {
			json.parse("[100,]");
			fail();
		} catch (JSONException e) {
			System.out.println(e);
			assertNotNull(e);
		}

		try {
			json.parse("[\"\0\"]");
			fail();
		} catch (JSONException e) {
			System.out.println(e);
			assertNotNull(e);
		}

		try {
			json.parse("{\"aaa\":}");
			fail();
		} catch (JSONException e) {
			System.out.println(e);
			assertNotNull(e);
		}

		try {
			json.parse("{\"aaa\":,\"bbb\":\"ccc\"}");
			fail();
		} catch (JSONException e) {
			System.out.println(e);
			assertNotNull(e);
		}

		try {
			json.parse("{} /**/");
			fail();
		} catch (JSONException e) {
			System.out.println(e);
			assertNotNull(e);
		}

		try {
			json.parse("{} #aaa");
			fail();
		} catch (JSONException e) {
			System.out.println(e);
			assertNotNull(e);
		}

		DateNumberTestClass dates = new DateNumberTestClass();
		dates.a = toDate(2000, 1, 1, 12, 5, 6, 0);
		dates.b = toDate(2001, 1, 1, 0, 0, 0, 0);
		dates.c = 1000;
		dates.d = 1001;

		json = new JSON();
		json.setDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
		json.setNumberFormat("000,000.00");
		assertEquals(dates, json.parse("{\"a\":\"2000/01/01 12:05:06.000\",\"b\":\"2001-01\",\"c\":\"001,000.00\",\"d\":\"1001.000\"}", DateNumberTestClass.class));

		json = new JSON();
		json.setDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
		assertEquals(dates, json.parse("{\"a\":\"2000/01/01 12:05:06.000\",\"b\":\"2001-01\",\"c\":\"1000\",\"d\":\"1001.000\"}", DateNumberTestClass.class));

		json = new JSON();
		json.setNumberFormat("000,000.00");
		assertEquals(dates, json.parse("{\"a\":" + dates.a.getTime() + ",\"b\":\"2001-01\",\"c\":\"001,000.00\",\"d\":\"1001.000\"}", DateNumberTestClass.class));

		json.setDateFormat(null);
		assertEquals(dates, json.parse("{\"a\":" + dates.a.getTime() + ",\"b\":\"2001-01\",\"c\":1000,\"d\":\"1001.000\"}", DateNumberTestClass.class));

		NamedTestClass named = new NamedTestClass();
		named.aaaAaaAaa = NamedTestEnum.aaaAaaAaa;
		named.AAA_BBB_CCC = NamedTestEnum.AAA_BBB_CCC;
		named.aaaあああ = NamedTestEnum.aaaあああ;

		json = new JSON();
		json.setPropertyStyle(NamingStyle.LOWER_CASE);
		json.setEnumStyle(NamingStyle.LOWER_CASE);
		assertEquals(named, json.parse("{\"aaaaaaaaa\":\"aaaaaaaaa\",\"aaa_bbb_ccc\":\"aaa_bbb_ccc\",\"aaaあああ\":\"aaaあああ\"}", NamedTestClass.class));

		json.setPropertyStyle(NamingStyle.LOWER_CAMEL);
		json.setEnumStyle(NamingStyle.LOWER_CAMEL);
		assertEquals(named, json.parse("{\"aaaAaaAaa\":\"aaaAaaAaa\",\"aaaBbbCcc\":\"aaaBbbCcc\",\"aaaあああ\":\"aaaあああ\"}", NamedTestClass.class));

		json.setPropertyStyle(NamingStyle.LOWER_UNDERSCORE);
		json.setEnumStyle(NamingStyle.LOWER_UNDERSCORE);
		assertEquals(named, json.parse("{\"aaa_aaa_aaa\":\"aaa_aaa_aaa\",\"aaa_bbb_ccc\":\"aaa_bbb_ccc\",\"aaaあああ\":\"aaaあああ\"}", NamedTestClass.class));

		json.setPropertyStyle(NamingStyle.UPPER_CASE);
		json.setEnumStyle(NamingStyle.UPPER_CASE);
		assertEquals(named, json.parse("{\"AAAAAAAAA\":\"AAAAAAAAA\",\"AAA_BBB_CCC\":\"AAA_BBB_CCC\",\"AAAあああ\":\"AAAあああ\"}", NamedTestClass.class));

		json.setPropertyStyle(NamingStyle.UPPER_CAMEL);
		json.setEnumStyle(NamingStyle.UPPER_CAMEL);
		assertEquals(named, json.parse("{\"AaaAaaAaa\":\"AaaAaaAaa\",\"AaaBbbCcc\":\"AaaBbbCcc\",\"Aaaあああ\":\"Aaaあああ\"}", NamedTestClass.class));

		json.setPropertyStyle(NamingStyle.UPPER_UNDERSCORE);
		json.setEnumStyle(NamingStyle.UPPER_UNDERSCORE);
		assertEquals(named, json.parse("{\"AAA_AAA_AAA\":\"AAA_AAA_AAA\",\"AAA_BBB_CCC\":\"AAA_BBB_CCC\",\"AAAあああ\":\"AAAあああ\"}", NamedTestClass.class));

		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("a", new BigDecimal("12"));

		List<BigDecimal> list4 = new ArrayList<BigDecimal>();
		list4.add(new BigDecimal("1"));
		list4.add(new BigDecimal("2"));
		list4.add(new BigDecimal("3"));
		list4.add(new BigDecimal("4"));
		list4.add(new BigDecimal("5"));
		result.put("b", list4);

        InputStream in = new ByteArrayInputStream("{\"a\": 12, \"b\": [1,2,3,4,5]}".getBytes()) {
        	@Override
        	public boolean markSupported() {
        		return false;
        	}
        };
        assertEquals(result, json.parse(in));
	}

	@Test
	public void testConvert() throws Exception {
		JSON json = new JSON();

		// boolean
		assertEquals(Boolean.TRUE, json.convert(100, boolean.class));
		assertEquals(Boolean.FALSE, json.convert(0, boolean.class));
		assertEquals(Boolean.FALSE, json.convert(0.0, boolean.class));
		assertEquals(Boolean.FALSE, json.convert(BigDecimal.ZERO, boolean.class));
		assertEquals(Boolean.FALSE, json.convert(BigInteger.ZERO, boolean.class));
		assertEquals(Boolean.FALSE, json.convert('0', boolean.class));
		assertEquals(Boolean.FALSE, json.convert("0", boolean.class));
		assertEquals(Boolean.FALSE, json.convert("f", boolean.class));
		assertEquals(Boolean.FALSE, json.convert("off", boolean.class));
		assertEquals(Boolean.FALSE, json.convert("no", boolean.class));
		assertEquals(Boolean.FALSE, json.convert("NaN", boolean.class));
		assertEquals(Boolean.FALSE, json.convert("false", boolean.class));
		assertEquals(Boolean.FALSE, json.convert("", boolean.class));
		assertEquals(Boolean.FALSE, json.convert(null, boolean.class));

		// Boolean
		assertEquals(Boolean.TRUE, json.convert(100, Boolean.class));
		assertEquals(Boolean.FALSE, json.convert(0, Boolean.class));
		assertEquals(Boolean.FALSE, json.convert("off", Boolean.class));
		assertEquals(Boolean.FALSE, json.convert("no", Boolean.class));
		assertEquals(Boolean.FALSE, json.convert("NaN", Boolean.class));
		assertEquals(Boolean.FALSE, json.convert("false", Boolean.class));
		assertEquals(Boolean.FALSE, json.convert("", Boolean.class));
		assertNull(json.convert(null, Boolean.class));

		// byte
		assertEquals((byte)0, json.convert(null, byte.class));
		assertEquals((byte)0, json.convert("0", byte.class));
		assertEquals((byte)0, json.convert("+0", byte.class));
		assertEquals((byte)0, json.convert("-0", byte.class));
		assertEquals((byte)5, json.convert(new BigDecimal("5"), byte.class));
		assertEquals((byte)5, json.convert(new BigDecimal("5.00"), byte.class));
		assertEquals((byte)0xFF, json.convert("0xFF", byte.class));
		assertEquals((byte)0xFF, json.convert("+0xFF", byte.class));
		try {
			json.convert(new BigDecimal("5.01"), byte.class);
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		// Byte
		assertEquals(null, json.convert(null, Byte.class));
		assertEquals((byte)0, json.convert("0", Byte.class));
		assertEquals((byte)0, json.convert("+0", Byte.class));
		assertEquals((byte)0, json.convert("-0", Byte.class));
		assertEquals((byte)5, json.convert(new BigDecimal("5"), Byte.class));
		assertEquals((byte)5, json.convert(new BigDecimal("5.00"), Byte.class));
		assertEquals((byte)0xFF, json.convert("0xFF", Byte.class));
		assertEquals((byte)0xFF, json.convert("+0xFF", Byte.class));
		try {
			json.convert(new BigDecimal("5.01"), Byte.class);
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		// short
		assertEquals((short)0, json.convert(null, short.class));
		assertEquals((short)0, json.convert("0", short.class));
		assertEquals((short)0, json.convert("+0", short.class));
		assertEquals((short)0, json.convert("-0", short.class));
		assertEquals((short)100, json.convert(new BigDecimal("100"), short.class));
		assertEquals((short)100, json.convert(new BigDecimal("100.00"), short.class));
		assertEquals((short)100, json.convert("100", short.class));
		assertEquals((short)100, json.convert("+100", short.class));
		assertEquals((short)0xFF, json.convert("0xFF", short.class));
		assertEquals((short)0xFF, json.convert("+0xFF", short.class));
		try {
			json.convert(new BigDecimal("100.01"), short.class);
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		// Short
		assertEquals(null, json.convert(null, Short.class));
		assertEquals((short)0, json.convert("0", Short.class));
		assertEquals((short)0, json.convert("+0", Short.class));
		assertEquals((short)0, json.convert("-0", Short.class));
		assertEquals((short)100, json.convert(new BigDecimal("100"), Short.class));
		assertEquals((short)100, json.convert(new BigDecimal("100.00"), Short.class));
		assertEquals((short)100, json.convert("100", Short.class));
		assertEquals((short)100, json.convert("+100", Short.class));
		assertEquals((short)0xFF, json.convert("0xFF", Short.class));
		assertEquals((short)0xFF, json.convert("+0xFF", Short.class));
		try {
			json.convert(new BigDecimal("100.01"), Short.class);
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		// int
		assertEquals(0, json.convert(null, int.class));
		assertEquals(0, json.convert("0", int.class));
		assertEquals(0, json.convert("+0", int.class));
		assertEquals(0, json.convert("-0", int.class));
		assertEquals(100, json.convert(new BigDecimal("100"), int.class));
		assertEquals(100, json.convert(new BigDecimal("100.00"), int.class));
		assertEquals(100, json.convert("100", int.class));
		assertEquals(100, json.convert("+100", int.class));
		assertEquals(0xFF, json.convert("0xFF", int.class));
		assertEquals(0xFF, json.convert("+0xFF", int.class));
		try {
			json.convert(new BigDecimal("100.01"), int.class);
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		// Integer
		assertEquals(null, json.convert(null, Integer.class));
		assertEquals(0, json.convert("0", Integer.class));
		assertEquals(0, json.convert("+0", Integer.class));
		assertEquals(0, json.convert("-0", Integer.class));
		assertEquals(100, json.convert(new BigDecimal("100"), Integer.class));
		assertEquals(100, json.convert(new BigDecimal("100.00"), Integer.class));
		assertEquals(100, json.convert("100", Integer.class));
		assertEquals(100, json.convert("+100", Integer.class));
		assertEquals(0xFF, json.convert("0xFF", Integer.class));
		assertEquals(0xFF, json.convert("+0xFF", Integer.class));
		try {
			json.convert(new BigDecimal("100.01"), Integer.class);
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		// long
		assertEquals(0l, json.convert(null, long.class));
		assertEquals(0l, json.convert("0", long.class));
		assertEquals(0l, json.convert("+0", long.class));
		assertEquals(0l, json.convert("-0", long.class));
		assertEquals(100l, json.convert(new BigDecimal("100"), long.class));
		assertEquals(100l, json.convert(new BigDecimal("100.00"), long.class));
		assertEquals(100l, json.convert("100", long.class));
		assertEquals(100l, json.convert("+100", long.class));
		assertEquals((long)0xFF, json.convert("0xFF", long.class));
		assertEquals((long)0xFF, json.convert("+0xFF", long.class));
		try {
			json.convert(new BigDecimal("100.01"), long.class);
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		// Long
		assertEquals(null, json.convert(null, Long.class));
		assertEquals(0l, json.convert("0", Long.class));
		assertEquals(0l, json.convert("+0", Long.class));
		assertEquals(0l, json.convert("-0", Long.class));
		assertEquals(100l, json.convert(new BigDecimal("100"), Long.class));
		assertEquals(100l, json.convert(new BigDecimal("100.00"), Long.class));
		assertEquals(100l, json.convert("100", Long.class));
		assertEquals(100l, json.convert("+100", Long.class));
		assertEquals((long)0xFF, json.convert("0xFF", Long.class));
		assertEquals((long)0xFF, json.convert("+0xFF", Long.class));
		try {
			json.convert(new BigDecimal("100.01"), Long.class);
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		// BigInteger
		assertEquals(null, json.convert(null, BigInteger.class));
		assertEquals(new BigInteger("100"), json.convert(new BigDecimal("100"), BigInteger.class));
		assertEquals(new BigInteger("100"), json.convert(new BigDecimal("100.00"), BigInteger.class));
		assertEquals(new BigInteger("100"), json.convert("100", BigInteger.class));
		assertEquals(new BigInteger("100"), json.convert("+100", BigInteger.class));
		assertEquals(new BigInteger("FF", 16), json.convert("0xFF", BigInteger.class));
		assertEquals(new BigInteger("FF", 16), json.convert("+0xFF", BigInteger.class));
		try {
			json.convert(new BigDecimal("100.01"), BigInteger.class);
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		// BigDecimal
		assertEquals(null, json.convert(null, BigDecimal.class));
		assertEquals(new BigDecimal("100"), json.convert(new BigDecimal("100"), BigDecimal.class));
		assertEquals(new BigDecimal("100.00"), json.convert(new BigDecimal("100.00"), BigDecimal.class));
		assertEquals(new BigDecimal("100"), json.convert("100", BigDecimal.class));
		assertEquals(new BigDecimal("100"), json.convert("+100", BigDecimal.class));
		assertEquals(new BigDecimal("100.01"), json.convert("100.01", BigDecimal.class));

		// Date
		assertEquals(toDate(1, 1, 1, 0, 0, 0, 0), json.convert("1", Date.class));
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convert("00", Date.class));
		assertEquals(toDate(1, 1, 1, 0, 0, 0, 0), json.convert("001", Date.class));
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convert("2000", Date.class));
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convert("200001", Date.class));
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convert("20000101", Date.class));

		assertEquals(toDate(2000, 1, 1, 12, 0, 0, 0), json.convert("2000010112", Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 0, 0), json.convert("200001011205", Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 0), json.convert("20000101120506", Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 0), json.convert("20000101120506+0900", Date.class));

		assertEquals(toDate(2000, 1, 1, 12, 0, 0, 0), json.convert("20000101T12", Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 0, 0), json.convert("20000101T1205", Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 0), json.convert("20000101T120506", Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 0), json.convert("20000101T120506+0900", Date.class));

		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convert("2000-01", Date.class));
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convert("2000-01-01", Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 0, 0, 0), json.convert("2000-01-01T12", Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 0, 0), json.convert("2000-01-01T12:05", Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 0, 0), json.convert("2000-01-01T12:05+09:00", Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 0), json.convert("2000-01-01T12:05:06", Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 0), json.convert("2000-01-01T12:05:06+09:00", Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 100), json.convert("2000-01-01T12:05:06.100", Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 100), json.convert("2000-01-01T12:05:06.100+09:00", Date.class));
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convert("2000年1月1日", Date.class));
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convert("2000年1月1日(月)", Date.class));

		assertEquals(toDate(2007, 12, 24, 20, 13, 15, 0), json.convert("Mon Dec 24 2007 20:13:15", Date.class));
		assertEquals(toDate(2007, 12, 24, 20, 13, 15, 0), json.convert("Mon Dec 24 2007 20:13:15 GMT+0900", Date.class));
		assertEquals(toDate(2007, 12, 24, 20, 13, 15, 0), json.convert("Mon, 24 Dec 2007 11:13:15 GMT", Date.class));
		assertEquals(toDate(2007, 12, 24, 20, 13, 54, 0), json.convert("Mon Dec 24 20:13:54 UTC+0900 2007", Date.class));
		assertEquals(toDate(2007, 12, 24, 20, 13, 54, 0), json.convert("Mon, 24 Dec 2007 11:13:54 UTC", Date.class));

		long t = toDate(2007, 12, 24, 0, 0, 0, 0).getTime();
		assertEquals(new java.sql.Date(t), json.convert("Mon Dec 24 2007 20:13:15", java.sql.Date.class));
		t = toDate(2007, 12, 24, 20, 13, 15, 0).getTime();
		assertEquals(new Timestamp(t), json.convert("Mon Dec 24 2007 20:13:15", Timestamp.class));
		t = toDate(1970, 1, 1, 20, 13, 15, 0).getTime();
		assertEquals(new Time(t), json.convert("20:13:15", Time.class));
		assertEquals(TimeZone.getTimeZone("JST"), json.convert("JST", TimeZone.class));

		assertEquals(ExampleEnum.Example1, json.convert("Example1", ExampleEnum.class));
		assertEquals(ExampleEnum.Example1, json.convert(1, ExampleEnum.class));
		assertEquals(ExampleEnum.Example1, json.convert("1", ExampleEnum.class));
		assertEquals(ExampleEnum.Example1, json.convert(true, ExampleEnum.class));
		assertEquals(ExampleEnum.Example0, json.convert(false, ExampleEnum.class));

		assertEquals(ExampleExtendEnum.Example1, json.convert("Example1", ExampleExtendEnum.class));
		assertEquals(ExampleExtendEnum.Example1, json.convert(1, ExampleExtendEnum.class));
		assertEquals(ExampleExtendEnum.Example1, json.convert("1", ExampleExtendEnum.class));
		assertEquals(ExampleExtendEnum.Example1, json.convert(true, ExampleExtendEnum.class));
		assertEquals(ExampleExtendEnum.Example0, json.convert(false, ExampleExtendEnum.class));

		try {
			json.convert("20100431", Date.class);
			fail();
		} catch (JSONException e) {
			System.out.println(e);
			assertNotNull(e);
		}

		try {
			json.convert(5, ExampleEnum.class);
			fail();
		} catch (JSONException e) {
			System.out.println(e);
			assertNotNull(e);
		}

		try {
			json.convert("aaa", int.class);
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		try {
			Object test = new Object() {
				public int aaa;
			};

			Map map = new LinkedHashMap();
			map.put("aaa", "aaa");
			json.convert(map, test.getClass());
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		// URI
		assertEquals(new URI("http://www.google.co.jp"), json.convert("http://www.google.co.jp", URI.class));
		assertEquals(new URI("/aaa/bbb.json"), json.convert("/aaa/bbb.json", URI.class));
		List uris = new ArrayList();
		uris.add("http://www.google.co.jp");
		uris.add("/aaa/bbb.json");
		assertEquals(new URI("http://www.google.co.jp"), json.convert(uris, URI.class));

		// URL
		assertEquals(new URL("http://www.google.co.jp"), json.convert("http://www.google.co.jp", URL.class));

		// File
		assertEquals(new File("./hoge.txt"), json.convert("./hoge.txt", File.class));
		assertEquals(new File("C:\\hoge.txt"), json.convert("C:\\hoge.txt", File.class));

		// InetAddress
		assertEquals(InetAddress.getByName("localhost"), json.convert("localhost", InetAddress.class));
		assertEquals(InetAddress.getByName("127.0.0.1"), json.convert("127.0.0.1", InetAddress.class));

		// Charset
		assertEquals(Charset.forName("UTF-8"), json.convert("UTF-8", Charset.class));

		// UUID
		UUID uuid = UUID.randomUUID();
		assertEquals(uuid, json.convert(uuid.toString(), UUID.class));

		// Map
		LinkedHashMap lhmap = new LinkedHashMap();
		lhmap.put("aaa", null);
		assertEquals(lhmap, json.convert("aaa", Map.class));

		// object
		try {
			Object test = new Object() {
				public int[] aaa;
			};

			Map map = new LinkedHashMap();
			ArrayList list = new ArrayList();
			list.add("aaa");
			map.put("aaa", list);
			json.convert(map, test.getClass());
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		// etc
		try {
			json.convert("aaa", Iterator.class);
			fail();
		} catch (JSONException e) {
			System.out.println(e);
			assertNotNull(e);
		}
	}

	@Test
	public void testBase64() throws Exception {
		JSON json = new JSON();

		Random rand = new Random();

		for (int i = 0; i < 100; i++) {
			byte[][] input = new byte[1][i];
			rand.nextBytes(input[0]);

			byte[][] output = (byte[][])json.parse(json.format(input), byte[][].class);
			assertEquals(toHexString(input[0]), toHexString(output[0]));
		}
	}

	private Date toDate(int year, int month, int date, int hour, int minute, int second, int msec) {
		Calendar c = Calendar.getInstance();
		c.clear();
		c.set(year, month-1, date, hour, minute, second);
		c.set(Calendar.MILLISECOND, msec);
		return c.getTime();
	}

	private String toHexString(byte[] data) {
		if (data == null) return "null";

		StringBuilder sb = new StringBuilder();
		for (byte d : data) {
			sb.append(Integer.toHexString((int)d & 0xFF));
			sb.append(" ");
		}
		return sb.toString();
	}

	public List<Integer> tx;

	@Test
	public void testGetRawType() throws Exception {

		List<BigDecimal> listA = new ArrayList<BigDecimal>();
		listA.add(new BigDecimal("1"));
		listA.add(new BigDecimal("2"));
		listA.add(new BigDecimal("3"));
		listA.add(new BigDecimal("4"));
		listA.add(new BigDecimal("5"));

		List<Integer> listB = new ArrayList<Integer>();
		listB.add(1);
		listB.add(2);
		listB.add(3);
		listB.add(4);
		listB.add(5);

		assertEquals(listA, JSON.decode("[1,2,3,4,5]", this.getClass().getField("tx").getType()));
		assertEquals(listB, JSON.decode("[1,2,3,4,5]", this.getClass().getField("tx").getGenericType()));

		assertEquals(listA, JSON.decode(new ByteArrayInputStream("[1,2,3,4,5]".getBytes("UTF-8")), this.getClass().getField("tx").getType()));
		assertEquals(listB, JSON.decode(new ByteArrayInputStream("[1,2,3,4,5]".getBytes("UTF-8")), this.getClass().getField("tx").getGenericType()));

		assertEquals(listA, JSON.decode(new StringReader("[1,2,3,4,5]"), this.getClass().getField("tx").getType()));
		assertEquals(listB, JSON.decode(new StringReader("[1,2,3,4,5]"), this.getClass().getField("tx").getGenericType()));

		JSON json = new JSON();
		assertEquals(listA, json.parse("[1,2,3,4,5]", this.getClass().getField("tx").getType()));
		assertEquals(listB, json.parse("[1,2,3,4,5]", this.getClass().getField("tx").getGenericType()));

		assertEquals(listA, json.parse(new ByteArrayInputStream("[1,2,3,4,5]".getBytes("UTF-8")), this.getClass().getField("tx").getType()));
		assertEquals(listB, json.parse(new ByteArrayInputStream("[1,2,3,4,5]".getBytes("UTF-8")), this.getClass().getField("tx").getGenericType()));

		assertEquals(listA, json.parse(new StringReader("[1,2,3,4,5]"), this.getClass().getField("tx").getType()));
		assertEquals(listB, json.parse(new StringReader("[1,2,3,4,5]"), this.getClass().getField("tx").getGenericType()));
	}

	@Test
	public void testValidate() throws Exception {
		JSON.validate(this.getClass().getResourceAsStream("Sample1.json"));
	}

}

class TestBeanWrapper {
	@JSONHint(type=Serializable.class)
	public TestBean test;
}

@SuppressWarnings("rawtypes")
class TestBean implements Serializable {
	private int a;
	public void setA(int a) { this.a = a; }
	public int getA() { return a; }

	public String b;
	public String getB() { return b; }

	private boolean c;
	public boolean isC() { return c; }
	public void setC(boolean c) { this.c = c; }

	public Date d;

	public Locale e;

	private Boolean f;
	public Boolean getF() { return f; }
	public void setF(Boolean f) { this.f = f; }

	private Pattern g;
	public Pattern getG() { return g; }
	public void setG(Pattern g) { this.g = g; }

	private TimeZone h;
	public TimeZone getH() { return h; }
	public void setH(TimeZone h) { this.h = h; }

	private String 漢字;
	public String get漢字() { return 漢字; }
	public void set漢字(String 漢字) { this.漢字 = 漢字; }

	@JSONHint(name="class")
	public Class class_;

	private String if_;
	public String getIf() { return if_; }
	public void setIf(String if_) { this.if_ = if_; }

	public BigDecimal _w = new  BigDecimal("100000000000000000000000");

	private int x = 10;
	int y = 100;
	protected int z = 1000;

	public void getignore(String test) {
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + a;
		result = prime * result + ((b == null) ? 0 : b.hashCode());
		result = prime * result + (c ? 1231 : 1237);
		result = prime * result + ((class_ == null) ? 0 : class_.hashCode());
		result = prime * result + ((d == null) ? 0 : d.hashCode());
		result = prime * result + ((e == null) ? 0 : e.hashCode());
		result = prime * result + ((f == null) ? 0 : f.hashCode());
		result = prime * result + ((g == null) ? 0 : g.pattern().hashCode());
		result = prime * result + ((h == null) ? 0 : h.hashCode());
		result = prime * result + ((if_ == null) ? 0 : if_.hashCode());
		result = prime * result + ((_w == null) ? 0 : _w.hashCode());
		result = prime * result + x;
		result = prime * result + y;
		result = prime * result + z;
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
		final TestBean other = (TestBean) obj;
		if (a != other.a)
			return false;
		if (b == null) {
			if (other.b != null)
				return false;
		} else if (!b.equals(other.b))
			return false;
		if (c != other.c)
			return false;
		if (class_ == null) {
			if (other.class_ != null)
				return false;
		} else if (!class_.equals(other.class_))
			return false;
		if (d == null) {
			if (other.d != null)
				return false;
		} else if (!d.equals(other.d))
			return false;
		if (e == null) {
			if (other.e != null)
				return false;
		} else if (!e.equals(other.e))
			return false;
		if (f == null) {
			if (other.f != null)
				return false;
		} else if (!f.equals(other.f))
			return false;
		if (g == null) {
			if (other.g != null)
				return false;
		} else if (!g.pattern().equals(other.g.pattern()))
			return false;
		if (h == null) {
			if (other.h != null)
				return false;
		} else if (!h.equals(other.h))
			return false;
		if (if_ == null) {
			if (other.if_ != null)
				return false;
		} else if (!if_.equals(other.if_))
			return false;
		if (_w == null) {
			if (other._w != null)
				return false;
		} else if (!_w.equals(other._w))
			return false;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		if (z != other.z)
			return false;
		return true;
	}

	public String toString() {
		return JSON.encode(this);
	}
}

class JavaBean {
	private String aname;
	private String bName;
	private String CName;

	public void setaname(String aname) {
		this.aname = aname;
	}

	public String getaname() {
		return aname;
	}

	public void setbName(String bName) {
		this.bName = bName;
	}

	public String getbName() {
		return bName;
	}

	public void setCName(String CName) {
		this.CName = CName;
	}

	public String getCName() {
		return CName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((CName == null) ? 0 : CName.hashCode());
		result = prime * result + ((aname == null) ? 0 : aname.hashCode());
		result = prime * result + ((bName == null) ? 0 : bName.hashCode());
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
		JavaBean other = (JavaBean) obj;
		if (CName == null) {
			if (other.CName != null)
				return false;
		} else if (!CName.equals(other.CName))
			return false;
		if (aname == null) {
			if (other.aname != null)
				return false;
		} else if (!aname.equals(other.aname))
			return false;
		if (bName == null) {
			if (other.bName != null)
				return false;
		} else if (!bName.equals(other.bName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "JavaBean [aname=" + aname
				+ ", bName=" + bName
				+ ", CName=" + CName
				+ "]";
	}
}

class NamedBean {
	public int namedPropertyAaa = 0;

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + namedPropertyAaa;
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
		final NamedBean other = (NamedBean) obj;
		if (namedPropertyAaa != other.namedPropertyAaa)
			return false;
		return true;
	}

	public String toString() {
		return JSON.encode(this);
	}
}

class NamedBean2 {
	public int aaaBbb;

	public int bbbAaa;

	@JSONHint(name = "bbbAaa")
	public void setAaaBbb(int value) {
		aaaBbb = value;
	}

	@JSONHint(name = "bbbAaa")
	public int getAaaBbb() {
		return aaaBbb;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + aaaBbb;
		result = prime * result + bbbAaa;
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
		NamedBean2 other = (NamedBean2) obj;
		if (aaaBbb != other.aaaBbb)
			return false;
		if (bbbAaa != other.bbbAaa)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "NamedBean2 [aaaBbb=" + aaaBbb + ", bbbAaa=" + bbbAaa + "]";
	}
}

class GenericsBean {
	private List<String> list = null;
	public InheritList ilist = null;
	public InheritList2 ilist2 = null;
	public InheritMap imap = null;
	public InheritMap2 imap2 = null;
	private Map<String, String> map = null;
	private List<List<String>> glist = null;
	public Map<String, Integer> map2 = null;
	public Map<Integer, String> map3 = null;

	public List<String> getList() {
		return list;
	}

	public void setList(List<String> list) {
		this.list = list;
	}

	public Map<String, String> getMap() {
		return map;
	}

	public void setMap(Map<String, String> map) {
		this.map = map;
	}

	public List<List<String>> getGenericsList() {
		return glist;
	}

	public void setGenericsList(List<List<String>> glist) {
		this.glist = glist;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((glist == null) ? 0 : glist.hashCode());
		result = PRIME * result + ((ilist == null) ? 0 : ilist.hashCode());
		result = PRIME * result + ((ilist2 == null) ? 0 : ilist2.hashCode());
		result = PRIME * result + ((imap == null) ? 0 : imap.hashCode());
		result = PRIME * result + ((imap2 == null) ? 0 : imap2.hashCode());
		result = PRIME * result + ((list == null) ? 0 : list.hashCode());
		result = PRIME * result + ((map == null) ? 0 : map.hashCode());
		result = PRIME * result + ((map2 == null) ? 0 : map2.hashCode());
		result = PRIME * result + ((map3 == null) ? 0 : map3.hashCode());
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
		final GenericsBean other = (GenericsBean) obj;
		if (glist == null) {
			if (other.glist != null)
				return false;
		} else if (!glist.equals(other.glist))
			return false;
		if (ilist == null) {
			if (other.ilist != null)
				return false;
		} else if (!ilist.equals(other.ilist))
			return false;
		if (ilist2 == null) {
			if (other.ilist2 != null)
				return false;
		} else if (!ilist2.equals(other.ilist2))
			return false;
		if (imap == null) {
			if (other.imap != null)
				return false;
		} else if (!imap.equals(other.imap))
			return false;
		if (imap2 == null) {
			if (other.imap2 != null)
				return false;
		} else if (!imap2.equals(other.imap2))
			return false;
		if (list == null) {
			if (other.list != null)
				return false;
		} else if (!list.equals(other.list))
			return false;
		if (map == null) {
			if (other.map != null)
				return false;
		} else if (!map.equals(other.map))
			return false;
		if (map2 == null) {
			if (other.map2 != null)
				return false;
		} else if (!map2.equals(other.map2))
			return false;
		if (map3 == null) {
			if (other.map3 != null)
				return false;
		} else if (!map3.equals(other.map3))
			return false;
		return true;
	}

	public String toString() {
		return JSON.encode(this);
	}
}

@SuppressWarnings("rawtypes")
class InheritedBean {
	public Map<String, Object> map0;
	public LinkedHashMap map1;
	public SuperLinkedHashMap map2;
	public List<Object> list0;
	public ArrayList list1;
	public SuperArrayList list2;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((list0 == null) ? 0 : list0.hashCode());
		result = prime * result + ((list1 == null) ? 0 : list1.hashCode());
		result = prime * result + ((list2 == null) ? 0 : list2.hashCode());
		result = prime * result + ((map0 == null) ? 0 : map0.hashCode());
		result = prime * result + ((map1 == null) ? 0 : map1.hashCode());
		result = prime * result + ((map2 == null) ? 0 : map2.hashCode());
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
		final InheritedBean other = (InheritedBean) obj;
		if (list0 == null) {
			if (other.list0 != null)
				return false;
		} else if (!list0.equals(other.list0))
			return false;
		if (list1 == null) {
			if (other.list1 != null)
				return false;
		} else if (!list1.equals(other.list1))
			return false;
		if (list2 == null) {
			if (other.list2 != null)
				return false;
		} else if (!list2.equals(other.list2))
			return false;
		if (map0 == null) {
			if (other.map0 != null)
				return false;
		} else if (!map0.equals(other.map0))
			return false;
		if (map1 == null) {
			if (other.map1 != null)
				return false;
		} else if (!map1.equals(other.map1))
			return false;
		if (map2 == null) {
			if (other.map2 != null)
				return false;
		} else if (!map2.equals(other.map2))
			return false;
		return true;
	}
}


class InheritList extends ArrayList<String> {

};

class InheritMap extends LinkedHashMap<Integer, String> {

};

class InheritList2 implements List<String> {
	private List<String> list = new ArrayList<String>();
	@Override
	public boolean add(String o) {
		return list.add(o);
	}

	@Override
	public void add(int i, String o) {
		list.add(o);
	}

	@Override
	public boolean addAll(Collection<? extends String> c) {
		return list.addAll(c);
	}

	@Override
	public boolean addAll(int i, Collection<? extends String> c) {
		return list.addAll(i, c);
	}

	@Override
	public void clear() {
		list.clear();
	}

	@Override
	public boolean contains(Object o) {
		return list.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return list.contains(c);
	}

	@Override
	public String get(int i) {
		return list.get(i);
	}

	@Override
	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	@Override
	public Iterator<String> iterator() {
		return list.iterator();
	}

	@Override
	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	@Override
	public ListIterator<String> listIterator() {
		return list.listIterator();
	}

	@Override
	public ListIterator<String> listIterator(int i) {
		return list.listIterator(i);
	}

	@Override
	public boolean remove(Object o) {
		return list.remove(o);
	}

	@Override
	public String remove(int i) {
		return list.remove(i);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return list.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return list.retainAll(c);
	}

	@Override
	public String set(int i, String o) {
		return list.set(i, o);
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public List<String> subList(int i, int j) {
		return list.subList(i, j);
	}

	@Override
	public Object[] toArray() {
		return list.toArray();
	}

	@Override
	public <T> T[] toArray(T[] array) {
		return list.toArray(array);
	}

	@Override
	public int hashCode() {
		return list.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof InheritList2) {
			return list.equals(((InheritList2)o).list);
		} else {
			return false;
		}
	};
}

class InheritMap2 implements Map<Integer, String> {
	Map<Integer, String> map = new LinkedHashMap<Integer, String>();

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<Integer, String>> entrySet() {
		return map.entrySet();
	}

	@Override
	public String get(Object key) {
		return map.get(key);
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Set<Integer> keySet() {
		return map.keySet();
	}

	@Override
	public String put(Integer key, String value) {
		return map.put(key, value);
	}

	@Override
	public void putAll(Map<? extends Integer, ? extends String> m) {
		map.putAll(m);
	}

	@Override
	public String remove(Object key) {
		return map.remove(key);
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public Collection<String> values() {
		return map.values();
	}

	@Override
	public int hashCode() {
		return map.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof InheritMap2) {
			return map.equals(((InheritMap2)obj).map);
		} else {
			return false;
		}
	}
}

@SuppressWarnings("rawtypes")
class SuperLinkedHashMap extends LinkedHashMap {
	private static final long serialVersionUID = 1L;
}

@SuppressWarnings("rawtypes")
class SuperArrayList extends ArrayList {
	private static final long serialVersionUID = 1L;
}

class AnnotationBean {
	@JSONHint(name="a")
	public int field;

	public int method;

	@JSONHint(name="b", format="###,###,000.0")
	public int getMethod() {
		return method;
	}

	@JSONHint(ignore=true)
	public int dummy;

	@JSONHint(format="yyyy/MM/dd")
	public Date date;

	@JSONHint(format="###,###,0.0")
	public int[] array1;

	@JSONHint(format="###,###,0.0")
	public Integer[] array2;

	@JSONHint(format="###,###,0.0", type=Vector.class)
	public List<Integer> array3;

	@JSONHint(serialized=true, ordinal=0)
	public String json_data;

	@JSONHint(serialized=true, ordinal=1)
	public String simple_json_data;

	@JSONHint(serialized=true, ordinal=2)
	public double number_json_data;

	@JSONHint(anonym="anonym")
	public AnonymTest anonymMap;

	@JSONHint(name = "name_a")
	public String namex = "aaa";

	public String getNamex() {
		return namex;
	}

	public void setNamex(String namex) {
		this.namex = namex;
	}

	@JSONHint(name = "name_b")
	public String namey = "aaa";

	@JSONHint(ignore = true)
	public String getNamey() {
		return namey;
	}

	@JSONHint(name = "name_c", ignore = true)
	public String namez = "aaa";

	public String getNamez() {
		return namey;
	}

	public void setNamez(String namey) {
		this.namey = namez;
	}

	@Override
	public int hashCode() {
		return ClassUtil.hashCode(this);
	}

	@Override
	public boolean equals(Object obj) {
		return ClassUtil.equals(this, obj);
	}

	@Override
	public String toString() {
		return ClassUtil.toString(this);
	}
}

class AnonymTest {
	public String anonym;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((anonym == null) ? 0 : anonym.hashCode());
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
		AnonymTest other = (AnonymTest) obj;
		if (anonym == null) {
			if (other.anonym != null)
				return false;
		} else if (!anonym.equals(other.anonym))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AnonymTest [anonym=" + anonym + "]";
	}
}

@SuppressWarnings({"rawtypes", "unchecked"})
class SuppressNullBean {
	public Object a = 100;
	public Object b = null;
	public List list = new ArrayList() {
		{
			add(100);
			add(null);
		}
	};
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((a == null) ? 0 : a.hashCode());
		result = prime * result + ((b == null) ? 0 : b.hashCode());
		result = prime * result + ((list == null) ? 0 : list.hashCode());
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
		SuppressNullBean other = (SuppressNullBean) obj;
		if (a == null) {
			if (other.a != null)
				return false;
		} else if (!a.equals(other.a))
			return false;
		if (b == null) {
			if (other.b != null)
				return false;
		} else if (!b.equals(other.b))
			return false;
		if (list == null) {
			if (other.list != null)
				return false;
		} else if (!list.equals(other.list))
			return false;
		return true;
	}

	public String toString() {
		return JSON.encode(this);
	}
};

enum ExampleEnum {
	Example0, Example1, Example2
}

enum ExampleExtendEnum {
	Example0 {},
	Example1 {},
	Example2 {}
}

class Point2DJSON extends JSON {
	protected Object preformat(Context context, Object value) throws Exception {
		if (value instanceof Point2D) {
			Point2D p = (Point2D) value;
			List<Double> list = new ArrayList<Double>();
			list.add(p.getX());
			list.add(p.getY());
			return list;
		}
		return super.preformat(context, value);
	}

	protected <T> T postparse(Context context, Object value,
			Class<? extends T> c, Type t) throws Exception {
		if (Point2D.class.isAssignableFrom(c) && value instanceof List<?>) {
			List<?> list = (List<?>)value;
			Point2D p = (Point2D) create(context, c);
			p.setLocation(context.convert(0, list.get(0), double.class),
					context.convert(1, list.get(1), double.class));
			return c.cast(p);
		}
		return super.postparse(context, value, c, t);
	}

	protected <T> T create(Context context, Class<? extends T> c)
			throws Exception {
		if (Point2D.class.isAssignableFrom(c)) {
	    	return c.cast(new Point2D.Double());
	    }
		return super.create(context, c);
	}

	protected boolean ignore(Context context, Class<?> c, Member m) {
		  return super.ignore(context, c, m);
	}
}

class StringBeanWrapper {
	@JSONHint(type=String.class)
	public StringBean sbean;

	@JSONHint(type=String.class)
	public Thread.State state;

	@JSONHint(type=String.class)
	public String text;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sbean == null) ? 0 : sbean.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		result = prime * result + ((text == null) ? 0 : text.hashCode());
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
		StringBeanWrapper other = (StringBeanWrapper) obj;
		if (sbean == null) {
			if (other.sbean != null)
				return false;
		} else if (!sbean.equals(other.sbean))
			return false;
		if (state == null) {
			if (other.state != null)
				return false;
		} else if (!state.equals(other.state))
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}
}

class StringBean {
	private String str;

	public StringBean(String str) {
		this.str = str;
	}

	public String toString() {
		return str;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((str == null) ? 0 : str.hashCode());
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
		StringBean other = (StringBean) obj;
		if (str == null) {
			if (other.str != null)
				return false;
		} else if (!str.equals(other.str))
			return false;
		return true;
	}
}

interface BaseInterface {
	Collection<?> getList();
}

class ImplClass implements BaseInterface {

	public List<?> getList() {
		return Arrays.asList("test");
	}
}

class GenericPropertyTest<T> {
	public T property;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((property == null) ? 0 : property.hashCode());
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
		GenericPropertyTest<?> other = (GenericPropertyTest<?>) obj;
		if (property == null) {
			if (other.property != null)
				return false;
		} else if (!property.equals(other.property))
			return false;
		return true;
	}
}

class GenericPropertyTestWrapper {
	public GenericPropertyTest<String> test = new GenericPropertyTest<String>();

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((test == null) ? 0 : test.hashCode());
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
		GenericPropertyTestWrapper other = (GenericPropertyTestWrapper) obj;
		if (test == null) {
			if (other.test != null)
				return false;
		} else if (!test.equals(other.test))
			return false;
		return true;
	}
}

class NullAppendable implements Appendable {

	@Override
	public Appendable append(CharSequence cs) throws IOException {
		return this;
	}

	@Override
	public Appendable append(char c) throws IOException {
		return this;
	}

	@Override
	public Appendable append(CharSequence cs, int start, int length)
			throws IOException {
		return this;
	}
}

class DateNumberTestClass {
	public Date a;

	@JSONHint(format="yyyy-MM")
	public Date b;

	public int c;

	@JSONHint(format="##0.000")
	public int d;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((a == null) ? 0 : a.hashCode());
		result = prime * result + ((b == null) ? 0 : b.hashCode());
		result = prime * result + c;
		result = prime * result + d;
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
		DateNumberTestClass other = (DateNumberTestClass) obj;
		if (a == null) {
			if (other.a != null)
				return false;
		} else if (!a.equals(other.a))
			return false;
		if (b == null) {
			if (other.b != null)
				return false;
		} else if (!b.equals(other.b))
			return false;
		if (c != other.c)
			return false;
		if (d != other.d)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DateNumberTestClass [a=" + a + ", b=" + b + ", c=" + c + ", d="
				+ d + "]";
	}
}

enum NamedTestEnum {
	aaaAaaAaa,
	AAA_BBB_CCC,
	aaaあああ {}
}

class NamedTestClass {
	public NamedTestEnum aaaAaaAaa;
	public NamedTestEnum AAA_BBB_CCC;
	public NamedTestEnum aaaあああ;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((AAA_BBB_CCC == null) ? 0 : AAA_BBB_CCC.hashCode());
		result = prime * result
				+ ((aaaAaaAaa == null) ? 0 : aaaAaaAaa.hashCode());
		result = prime * result + ((aaaあああ == null) ? 0 : aaaあああ.hashCode());
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
		NamedTestClass other = (NamedTestClass) obj;
		if (AAA_BBB_CCC == null) {
			if (other.AAA_BBB_CCC != null)
				return false;
		} else if (!AAA_BBB_CCC.equals(other.AAA_BBB_CCC))
			return false;
		if (aaaAaaAaa == null) {
			if (other.aaaAaaAaa != null)
				return false;
		} else if (!aaaAaaAaa.equals(other.aaaAaaAaa))
			return false;
		if (aaaあああ == null) {
			if (other.aaaあああ != null)
				return false;
		} else if (!aaaあああ.equals(other.aaaあああ))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "NamedTestClass [aaaAaaAaa=" + aaaAaaAaa + ", AAA_BBB_CCC="
				+ AAA_BBB_CCC + ", aaaあああ=" + aaaあああ + "]";
	}
}
