package net.arnx.jsonic.web;

import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import net.arnx.jsonic.JSON;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class RPCServletTest {
	
	private static Server server;
	
	@BeforeClass
	public static void init() throws Exception {
		new File("sample/basic/WEB-INF/database.dat").delete();
		new File("sample/seasar2/WEB-INF/database.dat").delete();
		new File("sample/spring/WEB-INF/database.dat").delete();
		new File("sample/guice/WEB-INF/database.dat").delete();
	
		server = new Server(16001);

		ContextHandlerCollection contexts = new ContextHandlerCollection();
		
		String[] systemClasses = new String[] {
				"org.apache.commons.",
				"org.aopalliance.",
				"ognl.",
				"javassist.",
				"net.arnx.",	
				"org.seasar.",
				"org.springframework.",
				"com.google.inject."
		};
		
		String[] serverClasses = new String[] {
		};
		
		WebAppContext basic = new WebAppContext("sample/basic", "/basic");
		basic.setSystemClasses(concat(basic.getSystemClasses(), systemClasses));
		basic.setServerClasses(concat(basic.getServerClasses(), serverClasses));
		contexts.addHandler(basic);
		
		WebAppContext seasar2 = new WebAppContext("sample/seasar2", "/seasar2");
		seasar2.setSystemClasses(concat(seasar2.getSystemClasses(), systemClasses));
		seasar2.setServerClasses(concat(seasar2.getServerClasses(), serverClasses));
		contexts.addHandler(seasar2);
		
		WebAppContext spring = new WebAppContext("sample/spring", "/spring");
		spring.setSystemClasses(concat(spring.getSystemClasses(), systemClasses));
		spring.setServerClasses(concat(spring.getServerClasses(), serverClasses));
		contexts.addHandler(spring);
		
		WebAppContext guice = new WebAppContext("sample/guice", "/guice");
		guice.setSystemClasses(concat(guice.getSystemClasses(), systemClasses));
		guice.setServerClasses(concat(guice.getServerClasses(), serverClasses));
		contexts.addHandler(guice);
		
		server.setHandler(contexts);
		server.start();
	}
	
	private static String[] concat(String[] a, String[] b) {
		String[] result = new String[a.length + b.length];
		System.arraycopy(a, 0, result, 0, a.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		return result;
	}
	
	@AfterClass
	public static void destroy() throws Exception {
		server.stop();
	}
	
	
	@Test
	public void testRPC() throws Exception {
		testRPC("basic");
	}
	
	@Test
	public void testRPCwithSeasar2() throws Exception {
		testRPC("seasar2");
	}
	
	@Test
	public void testRPCwithSpring() throws Exception {
		testRPC("spring");
	}
	
	@Test
	public void testRPCwithGuice() throws Exception {
		testRPC("guice");
	}
	
	public void testRPC(String app) throws Exception {
		System.out.println("\n<<START testRPC: " + app + ">>");
		
		URL url = new URL("http://localhost:16001/" + app + "/rpc/rpc.json");
		HttpURLConnection con = null;
		
		// GET
		con = (HttpURLConnection)url.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("Content-Type", "application/json");
		con.connect();
		assertEquals(SC_METHOD_NOT_ALLOWED, con.getResponseCode());
		con.disconnect();
		
		// POST
		con = (HttpURLConnection)url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "");
		con.connect();
		assertEquals(SC_OK, con.getResponseCode());
		assertEquals(JSON.decode("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32600,\"message\":\"Invalid Request.\",\"data\":{\"message\":\"Request is empty.\"}},\"id\":null}"), 
				JSON.decode(read(con.getInputStream())));
		con.disconnect();

		con = (HttpURLConnection)url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "{\"method\":\"calc.plus\",\"params\":[1,2],\"id\":1}");
		con.connect();
		assertEquals(SC_OK, con.getResponseCode());
		assertEquals(JSON.decode("{\"result\":3,\"error\":null,\"id\":1}"), 
				JSON.decode(read(con.getInputStream())));
		con.disconnect();

		con = (HttpURLConnection)url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "{\"method\":\"calc.init\",\"params\":[],\"id\":1}");
		con.connect();
		assertEquals(SC_OK, con.getResponseCode());
		assertEquals(JSON.decode("{\"result\":null,\"error\":{\"code\":-32601,\"message\":\"Method not found.\",\"data\":{\"message\":\"Method not found: calc.init\"}},\"id\":1}"), 
				JSON.decode(read(con.getInputStream())));
		con.disconnect();
		con = (HttpURLConnection)url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "{\"method\":\"calc.destroy\",\"params\":[],\"id\":1}");
		con.connect();
		assertEquals(SC_OK, con.getResponseCode());
		assertEquals(JSON.decode("{\"result\":null,\"error\":{\"code\":-32601,\"message\":\"Method not found.\",\"data\":{\"message\":\"Method not found: calc.destroy\"}},\"id\":1}"), 
				JSON.decode(read(con.getInputStream())));
		con.disconnect();
		
		// PUT
		con = (HttpURLConnection)url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("PUT");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "");
		con.connect();
		assertEquals(SC_METHOD_NOT_ALLOWED, con.getResponseCode());
		con.disconnect();
		
		// DELETE
		con = (HttpURLConnection)url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("DELETE");
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Content-Length", "0");
		con.connect();
		assertEquals(SC_METHOD_NOT_ALLOWED, con.getResponseCode());
		con.disconnect();
		
		System.out.println("<<END testRPC: " + app + ">>\n");
	}
	
	private static void write(HttpURLConnection con, String text) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(con.getOutputStream(), "UTF-8"));
		writer.write(text);
		writer.flush();
		writer.close();
	}
	
	private static String read(InputStream in) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		StringBuilder sb = new StringBuilder();
		char[] cb = new char[1024];
		int length = 0; 
		while ((length = reader.read(cb)) != -1) {
			sb.append(cb, 0, length);
		}
		reader.close();
		return sb.toString();
	}
}