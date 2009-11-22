/*
 * Copyright 2007-2008 Hidekatsu Izuno
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
package net.arnx.jsonic.web;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.arnx.jsonic.JSON;

public class Container {
	public Boolean debug = false;
	public String init = "init";
	public String destroy = "destroy";
	
	private ServletConfig config;
	private ServletContext context;
	
	public void init(ServletConfig config) {
		this.config = config;
		this.context = config.getServletContext();
	}

	public boolean isDebugMode() {
		return (debug != null) ? debug : false;
	}
	
	public Object getComponent(String className, HttpServletRequest request, HttpServletResponse response) 
		throws Exception {
		
		Object o = findClass(className).newInstance();

		for (Field field : o.getClass().getFields()) {
			Class<?> c = field.getType();
			if (ServletContext.class.equals(c) && "application".equals(field.getName())) {
				field.set(o, context);
			} else if (ServletConfig.class.equals(c) && "config".equals(field.getName())) {
				field.set(o, config);
			} else if (HttpServletRequest.class.equals(c) && "request".equals(field.getName())) {
				field.set(o, request);
			} else if (HttpServletResponse.class.equals(c)	&& "response".equals(field.getName())) {
				field.set(o, response);
			} else if (HttpSession.class.equals(c) && "session".equals(field.getName())) {
				field.set(o, request.getSession(true));
			}
		}
		
		return o;
	}
	
	protected static Class<?> findClass(String name) throws ClassNotFoundException {
		Class<?> c = null;
		try {
			c = Class.forName(name, true, Thread.currentThread().getContextClassLoader());
		} catch (ClassNotFoundException e) {
			try {
				c = Class.forName(name, true, Container.class.getClassLoader());
			} catch (ClassNotFoundException e2) {
				c = Class.forName(name);				
			}
		}
		
		return c;
	}
	
	/**
	 * Called before invoking the target method.
	 * 
	 * @param target The target instance.
	 * @param params The parameters of the target method.
	 * @return The parameters before processing.
	 */
	public Object[] preinvoke(Object target, Object... params) throws Exception {
		return params;
	}
	
	public Method findMethod(Object o, String methodName, List<?> args) throws NoSuchMethodException {
		if (args == null) args = Collections.emptyList();
		
		methodName = toLowerCamel(methodName);
		
		Class<?> c = o.getClass();
		
		Method method = null;
		Type[] paramTypes = null;
		
		for (Method m : c.getMethods()) {
			if (Modifier.isStatic(m.getModifiers())) continue;
			
			if (m.getName().equals(methodName)) {
				Type[] pTypes = m.getGenericParameterTypes();
				if (args.size() <= Math.max(1, pTypes.length)) {
					if (method == null || Math.abs(args.size() - pTypes.length) < Math.abs(args.size() - paramTypes.length)) {
						method = m;
						paramTypes = pTypes;
					} else if (pTypes.length == paramTypes.length) {
						throw new IllegalStateException("too many methods found: " + toPrintString(c, methodName, args));
					}
				}
			}
		}
		
		if (method == null || limit(c, method)) {
			throw new NoSuchMethodException("method missing: " + toPrintString(c, methodName, args));
		}
		
		return method;
	}
	
	/**
	 * Called after invoked the target method.
	 * 
	 * @param target The target instance.
	 * @param result The returned value of the target method call.
	 * @return The returned value after processed.
	 */
	public Object postinvoke(Object target, Object result) throws Exception {
		return result;
	}
	
	protected boolean limit(Class<?> c, Method method) {
		return method.getDeclaringClass().equals(Object.class);
	}
	
	public void debug(String message) {
		debug(message, null);
	}
	
	public void debug(String message, Throwable e) {
		if (!isDebugMode()) return;
		
		if (e != null) {
			context.log(message, e);
		} else {
			context.log(message);
		}
	}
	
	public void error(String message, Throwable e) {
		if (e != null) {
			context.log(message, e);
		} else {
			context.log(message);
		}
	}

	public void destory() {
	}
	
	private static String toPrintString(Class<?> c, String methodName, List<?> args) {
		StringBuilder sb = new StringBuilder(c.getName());
		sb.append('#').append(methodName).append('(');
		if (args != null) {
			String str = JSON.encode(args);
			sb.append(str, 1, str.length()-1);
		}
		sb.append(')');
		return sb.toString();
	}
	
	private static String toLowerCamel(String name) {
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
		if (sb.length() > 1 && Character.isUpperCase(sb.charAt(0)) && Character.isLowerCase(sb.charAt(1))) {
			sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
		}
		return sb.toString();
	}
}