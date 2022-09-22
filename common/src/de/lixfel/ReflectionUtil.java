// SPDX-License-Identifier: MIT

package de.lixfel;

import org.bukkit.Bukkit;

import java.lang.reflect.*;
import java.util.Arrays;

// ReflectionUtil heavily inspired by TinyProtocol
public final class ReflectionUtil {
	private ReflectionUtil() {}

	private static final String ORG_BUKKIT_CRAFTBUKKIT;
	private static final boolean REPLACE_NET_MINECRAFT;
	static {
		String craftbukkitPackage;
		boolean legacyNms;
		try {
			craftbukkitPackage = Bukkit.getServer().getClass().getPackage().getName();
			legacyNms = Integer.parseInt(craftbukkitPackage.split("[.](?=[^.]*$)")[1].split("_")[1]) < 17;
		} catch (NoClassDefFoundError e) {
			craftbukkitPackage = "";
			legacyNms = false;
		}
		ORG_BUKKIT_CRAFTBUKKIT = craftbukkitPackage;
		REPLACE_NET_MINECRAFT = legacyNms;
	}

	private static final String LEGACY_NET_MINECRAFT_SERVER = ORG_BUKKIT_CRAFTBUKKIT.replace("org.bukkit.craftbukkit", "net.minecraft.server");

	public static <T> FieldWrapper<T> getField(Class<?> target, Class<T> fieldType, String name) {
		return getField(target, fieldType, name, 0);
	}

	public static <T> FieldWrapper<T> getField(Class<?> target, Class<T> fieldType, int index, Class<?>... parameters) {
		return getField(target, fieldType, null, index, parameters);
	}

	private static <T> FieldWrapper<T> getField(Class<?> target, Class<T> fieldType, String name, int index, Class<?>... parameters) {
		for (final Field field : target.getDeclaredFields()) {
			if(matching(field, name, fieldType, parameters) && index-- <= 0) {
				field.setAccessible(true);

				return new FieldWrapper<T>() {
					@Override
					@SuppressWarnings("unchecked")
					public T get(Object target) {
						try {
							return (T) field.get(target);
						} catch (IllegalAccessException e) {
							throw new IllegalStateException("Access denied", e);
						}
					}

					@Override
					public void set(Object target, Object value) {
						try {
							field.set(target, value);
						} catch (IllegalAccessException e) {
							throw new IllegalStateException("Access denied", e);
						}
					}
				};
			}
		}

		if (target.getSuperclass() != null)
			return getField(target.getSuperclass(), fieldType, name, index);

		throw new IllegalArgumentException("Field not found");
	}

	private static <T> boolean matching(Field field, String name, Class<T> fieldType, Class<?>... parameters) {
		if(name != null && !field.getName().equals(name))
			return false;

		if(!fieldType.isAssignableFrom(field.getType()))
			return false;

		if(parameters.length > 0) {
			Type[] arguments = ((ParameterizedType)field.getGenericType()).getActualTypeArguments();

			for(int i = 0; i < parameters.length; i++) {
				if(arguments[i] != parameters[i])
					return false;
			}
		}
		return true;
	}

	public static MethodWrapper getMethod(Class<?> clazz, String methodName, Class<?>... args) {
		return getMethod(clazz, null, methodName, args);
	}

	public static MethodWrapper getMethod(Class<?> clazz, Class<?> returnType, String methodName, Class<?>... args) {
		for (final Method method : clazz.getDeclaredMethods()) {
			if ((methodName == null || method.getName().equals(methodName))
					&& (returnType == null || method.getReturnType().equals(returnType))
					&& Arrays.equals(method.getParameterTypes(), args)) {
				method.setAccessible(true);

				return (target, arguments) -> {
					try {
						return method.invoke(target, arguments);
					} catch (Exception e) {
						throw new RuntimeException("Exception on call", e);
					}
				};
			}
		}

		if (clazz.getSuperclass() != null)
			return getMethod(clazz.getSuperclass(), methodName, args);

		throw new IllegalStateException("Method not found");
	}

	public static Class<?> getClass(String name) {
		try {
			if(name.startsWith("org.bukkit.craftbukkit")) {
				return Class.forName(ORG_BUKKIT_CRAFTBUKKIT + name.substring(22));
			} else if(REPLACE_NET_MINECRAFT && name.startsWith("net.minecraft")) {
				return Class.forName(LEGACY_NET_MINECRAFT_SERVER + "." + name.split("[.](?=[^.]*$)")[1]);
			} else {
				return Class.forName(name);
			}
		} catch (ClassNotFoundException e) {
			throw new NoClassDefFoundError();
		}
	}

	public interface MethodWrapper {
		Object invoke(Object target, Object... arguments);
	}

	public interface FieldWrapper<T> {
		T get(Object target);

		void set(Object target, Object value);
	}
}
