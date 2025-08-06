package de.morihofi.certgine.core.modules;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ServiceLoaderWithArgs<T> {

    private final Class<T> serviceType;
    private final Class<?>[] argTypes;
    private final Object[] args;

    public ServiceLoaderWithArgs(Class<T> serviceType, Class<?>[] argTypes, Object[] args) {
        this.serviceType = serviceType;
        this.argTypes = argTypes;
        this.args = args;
    }

    public List<T> load() {
        List<T> instances = new ArrayList<>();
        String resource = "META-INF/services/" + serviceType.getName();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            var resources = classLoader.getResources(resource);

            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String className = line.strip();
                        if (!className.isEmpty() && !className.startsWith("#")) {
                            Class<?> implClass = Class.forName(className, true, classLoader);
                            if (!serviceType.isAssignableFrom(implClass)) {
                                throw new IllegalArgumentException(className + " is not a subtype of " + serviceType);
                            }

                            Constructor<?> ctor = implClass.getConstructor(argTypes);
                            @SuppressWarnings("unchecked")
                            T instance = (T) ctor.newInstance(args);
                            instances.add(instance);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load service implementations for: " + serviceType.getName(), e);
        }

        return instances;
    }
}

