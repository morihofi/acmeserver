package de.morihofi.acmeserver.configPreprocessor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.configPreprocessor.annotation.ConfigurationClassExtends;
import de.morihofi.acmeserver.configPreprocessor.annotation.ConfigurationField;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
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

     //   System.out.println(json);
    }

    private static Map<String, Object> processConfigClass(Class<?> configClass) {
        Map<String, Object> jsonMap = new HashMap<>();
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(configClass.getPackage().getName()))
                .setScanners(Scanners.FieldsAnnotated, Scanners.SubTypes));

        // Process fields from the entire class hierarchy
        processFields(configClass, jsonMap, reflections);

        return jsonMap;
    }

    private static void processFields(Class<?> configClass, Map<String, Object> jsonMap, Reflections reflections) {
        if (configClass == null || configClass == Object.class) {
            return;
        }

        // Recursively process superclass fields first
        processFields(configClass.getSuperclass(), jsonMap, reflections);

        for (Field field : configClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(ConfigurationField.class)) {
                ConfigurationField annotation = field.getAnnotation(ConfigurationField.class);

                if(annotation.hideVisibility()) {
                    continue;
                }

                Map<String, Object> fieldInfo = new HashMap<>();
                fieldInfo.put("description", annotation.name());
                fieldInfo.put("deprecated", field.isAnnotationPresent(Deprecated.class));
                fieldInfo.put("required", annotation.required());

                Class<?> fieldType = field.getType();
                if (List.class.isAssignableFrom(fieldType)) {
                    fieldInfo.put("type", getGenericType(field));
                    fieldInfo.put("isList", true);
                    fieldInfo.put("fields", processGenericListType(field));
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

    private static Map<String, Object> processGenericListType(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType parameterizedType) {
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length > 0) {
                Class<?> listType = (Class<?>) actualTypeArguments[0];
                return processConfigClass(listType);
            }
        }
        return new HashMap<>();
    }

    private static boolean isComplexType(Class<?> fieldType) {
        return !fieldType.isPrimitive() && !fieldType.getName().startsWith("java.lang");
    }

    private static Map<String, Object> getSubTypesInfo(Set<Class<?>> subTypes) {
        Map<String, Object> subTypesInfo = new HashMap<>();
        for (Class<?> subType : subTypes) {
            if (!Modifier.isAbstract(subType.getModifiers())) {
                ConfigurationClassExtends classExtendsAnnotation = subType.getAnnotation(ConfigurationClassExtends.class);
                if (classExtendsAnnotation == null) {
                    throw new IllegalArgumentException("Subclass " + subType.getSimpleName() + " doesn't have @ConfigurationClassExtends annotation");
                }
                Map<String, Object> subTypeInfo = new HashMap<>();
                subTypeInfo.put("fields", processConfigClass(subType));
                subTypesInfo.put(!classExtendsAnnotation.configName().isEmpty() ? classExtendsAnnotation.configName() : subType.getSimpleName(),
                        subTypeInfo);
            }
        }
        return subTypesInfo;
    }
}
