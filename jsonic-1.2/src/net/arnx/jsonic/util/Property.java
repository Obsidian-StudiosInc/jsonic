package net.arnx.jsonic.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

public class Property implements Comparable<Property> {
	private Class<?> beanClass;
	
	private String name;
	
	Field field;	
	Method readMethod;
	Method writeMethod;
	
	public Property(Class<?> beanClass, String name, Field field, Method readMethod, Method writeMethod) {
		this.beanClass = beanClass;
		this.name = name;
		this.field = field;
		this.readMethod = readMethod;
		this.writeMethod = writeMethod;
	}
	
	public Class<?> getBeanClass() {
		return beanClass;
	}
	
	public String getName() {
		return name;
	}
	
	public Field getField() {
		return field;
	}
	
	public Method getReadMethod() {
		return readMethod;
	}
	
	public Method getWriteMethod() {
		return writeMethod;
	}
	
	public boolean isReadable() {
		return (readMethod != null || field != null);
	}
	
	public Member getReadMember() {
		if (readMethod != null) {
			return readMethod;
		} else if (field != null) {
			return field;
		} else {
			throw new IllegalStateException(name + " property is not readable.");
		}
	}
	
	public Class<?> getReadType() {
		if (readMethod != null) {
			return readMethod.getReturnType();
		} else if (field != null) {
			return field.getType();
		} else {
			throw new IllegalStateException(name + " property is not readable.");
		}
	}
	
	public Type getReadGenericType() {
		if (readMethod != null) {
			return readMethod.getGenericReturnType();
		} else if (field != null) {
			return field.getGenericType();
		} else {
			throw new IllegalStateException(name + " property is not readable.");
		}
	}
	
	public <T extends Annotation> T getReadAnnotation(Class<T> annotationClass) {
		if (readMethod != null) {
			return readMethod.getAnnotation(annotationClass);
		} else if (field != null) {
			return field.getAnnotation(annotationClass);
		} else {
			throw new IllegalStateException(name + " property is not readable.");
		}
	}
	
	public Object get(Object target) {
		try {
			if (readMethod != null) {
				try {
					return readMethod.invoke(target);
				} catch (InvocationTargetException e) {
					if (e.getCause() instanceof Error) {
						throw (Error)e.getCause();
					} else if (e.getCause() instanceof RuntimeException) {
						throw (RuntimeException)e.getCause();
					} else {
						throw new IllegalStateException(e.getCause());
					}
				}
			} else if (field != null) {
				return field.get(target);
			} else {
				throw new IllegalStateException(name + " property is not readable.");
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	public boolean isWritable() {
		return (writeMethod != null || (field != null && !Modifier.isFinal(field.getModifiers())));
	}
	
	public Member getWriteMember() {
		if (writeMethod != null) {
			return writeMethod;
		} else if (field != null && !Modifier.isFinal(field.getModifiers())) {
			return field;
		} else {
			throw new IllegalStateException(name + " property is not writable.");
		}
	}
	
	public Class<?> getWriteType() {
		if (writeMethod != null) {
			return writeMethod.getParameterTypes()[0];
		} else if (field != null && !Modifier.isFinal(field.getModifiers())) {
			return field.getType();
		} else {
			throw new IllegalStateException(name + " property is not writable.");
		}
	}
	
	public Type getWriteGenericType() {
		if (writeMethod != null) {
			return writeMethod.getGenericParameterTypes()[0];
		} else if (field != null && !Modifier.isFinal(field.getModifiers())) {
			return field.getGenericType();
		} else {
			throw new IllegalStateException(name + " property is not writable.");
		}
	}
	
	public <T extends Annotation> T getWriteAnnotation(Class<T> annotationClass) {
		if (writeMethod != null) {
			return writeMethod.getAnnotation(annotationClass);
		} else if (field != null && !Modifier.isFinal(field.getModifiers())) {
			return field.getAnnotation(annotationClass);
		} else {
			throw new IllegalStateException(name + " property is not writable.");
		}
	}
	
	public void set(Object target, Object value) {
		try {
			if (writeMethod != null) {
				try {
					writeMethod.invoke(target, value);
				} catch (InvocationTargetException e) {
					if (e.getCause() instanceof Error) {
						throw (Error)e.getCause();
					} else if (e.getCause() instanceof RuntimeException) {
						throw (RuntimeException)e.getCause();
					} else {
						throw new IllegalStateException(e.getCause());
					}
				}
			} else if (field != null && !Modifier.isFinal(field.getModifiers())) {
				field.set(target, value);
			} else {
				throw new IllegalStateException(name + " property is not writable.");
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	@Override
	public int compareTo(Property property) {
		if (!beanClass.equals(property.beanClass)) {
			return beanClass.getName().compareTo(property.beanClass.getName());			
		} else {
			return name.compareTo(property.name);
		}
	}

	@Override
	public String toString() {
		return "Property [beanClass=" + beanClass + ", name=" + name
				+ ", field=" + field + ", readMethod=" + readMethod
				+ ", writeMethod=" + writeMethod + "]";
	}
}