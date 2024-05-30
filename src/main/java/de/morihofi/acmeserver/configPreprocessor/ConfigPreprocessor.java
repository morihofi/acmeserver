/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.morihofi.acmeserver.configPreprocessor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.configPreprocessor.annotation.ConfigurationField;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigPreprocessor {

    public static String json = null;

    public static void preprocessConfig() {
        Map<String, Object> jsonMap = processConfigClass(Main.appConfig.getClass());

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        json = gson.toJson(jsonMap);

        System.out.println(json);
    }

    private static Map<String, Object> processConfigClass(Class<?> configClass) {
        Map<String, Object> jsonMap = new HashMap<>();
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(configClass.getPackage().getName()))
                .setScanners(new FieldAnnotationsScanner(), new SubTypesScanner()));

        for (Field field : configClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(ConfigurationField.class)) {
                ConfigurationField annotation = field.getAnnotation(ConfigurationField.class);
                Map<String, Object> fieldInfo = new HashMap<>();
                fieldInfo.put("description", annotation.name());
                fieldInfo.put("deprecated", configClass.isAnnotationPresent(Deprecated.class));

                Class<?> fieldType = field.getType();
                if (List.class.isAssignableFrom(fieldType)) {
                    fieldInfo.put("type", getGenericType(field));
                    fieldInfo.put("isList", true);
                } else {
                    fieldInfo.put("type", fieldType.getSimpleName());
                    fieldInfo.put("isList", false);

                    if (Modifier.isAbstract(fieldType.getModifiers())) {
                        Set<Class<?>> subTypes = reflections.getSubTypesOf((Class<Object>) fieldType);
                        fieldInfo.put("subTypes", getSubTypesInfo(subTypes));
                    } else if (isComplexType(fieldType)) {
                        fieldInfo.put("fields", processConfigClass(fieldType));
                    }
                }

                jsonMap.put(field.getName(), fieldInfo);
            }
        }
        return jsonMap;
    }

    private static String getGenericType(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType parameterizedType) {
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length > 0) {
                return ((Class<?>) actualTypeArguments[0]).getSimpleName();
            }
        }
        return "Object";
    }

    private static boolean isComplexType(Class<?> fieldType) {
        return !fieldType.isPrimitive() && !fieldType.getName().startsWith("java.lang");
    }

    private static Map<String, Object> getSubTypesInfo(Set<Class<?>> subTypes) {
        Map<String, Object> subTypesInfo = new HashMap<>();
        for (Class<?> subType : subTypes) {
            if (!Modifier.isAbstract(subType.getModifiers())) {
                subTypesInfo.put(subType.getSimpleName(), processConfigClass(subType));
            }
        }
        return subTypesInfo;
    }
}
