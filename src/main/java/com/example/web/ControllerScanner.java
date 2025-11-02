package com.example.web;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import com.example.annotation.AnnotationController;
import com.example.annotation.GetMethode;

public class ControllerScanner {

    private static final Map<String, Method> routes = new HashMap<>();
    private static final Map<Method, Object> instances = new HashMap<>();


    public static void initialize(String basePackagePath) {
        try {
            scanAndRegister(basePackagePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void scanAndRegister(String packageName) throws Exception {
        String path = packageName.replace('.', '/');
        File directory = new File(
            Objects.requireNonNull(
                Thread.currentThread().getContextClassLoader().getResource(path)
            ).toURI()
        );

        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isDirectory()) {
                scanAndRegister(packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                Class<?> cls = Class.forName(className);

                if (cls.isAnnotationPresent(AnnotationController.class)) {
                    AnnotationController controllerAnno = cls.getAnnotation(AnnotationController.class);
                    String baseRoute = controllerAnno.value();
                    Object instance = cls.getDeclaredConstructor().newInstance();

                    boolean foundAnnotatedMethod = false;

                    for (Method method : cls.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(GetMethode.class)) {
                            GetMethode getAnno = method.getAnnotation(GetMethode.class);
                            String fullRoute = baseRoute + getAnno.value();
                            routes.put(fullRoute, method);
                            instances.put(method, instance);
                            foundAnnotatedMethod = true;
                            System.out.println("Registered route: " + fullRoute + " → " + cls.getSimpleName() + "." + method.getName() + "()");
                        }
                    }

                    if (!foundAnnotatedMethod) {
                        try {
                            Method handle = cls.getMethod(
                                "handle",
                                javax.servlet.http.HttpServletRequest.class,
                                javax.servlet.http.HttpServletResponse.class
                            );
                            routes.put(baseRoute, handle);
                            instances.put(handle, instance);
                            System.out.println("Registered default controller: " + baseRoute + " → " + cls.getSimpleName() + ".handle()");
                        } catch (NoSuchMethodException e) {
                            System.err.println("No @GetMethode or handle() found in " + cls.getSimpleName());
                        }
                    }
                }
            }
        }
    }

    public static Method getMethod(String path) {
        return routes.get(path);
    }

    public static Object getController(Method method) {
        return instances.get(method);
    }

    public static void listMethods(String basePackage) {
        try {
            scanAndList(basePackage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void scanAndList(String packageName) throws Exception {
        String path = packageName.replace('.', '/');
        File directory = new File(
            Objects.requireNonNull(
                Thread.currentThread().getContextClassLoader().getResource(path)
            ).toURI()
        );

        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isDirectory()) {
                scanAndList(packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                Class<?> cls = Class.forName(className);

                System.out.println("\nClass: " + cls.getName());
                Method[] methods = cls.getDeclaredMethods();
                if (methods.length == 0) {
                    System.out.println("   (No methods found)");
                } else {
                    for (Method m : methods) {
                        String annotation = m.isAnnotationPresent(GetMethode.class) ? " @GetMethode" : "";
                        System.out.println("   → " + m.getName() + annotation);
                    }
                }
            }
        }
    }

    public static void printAllRoutes() {
        for (Map.Entry<String, Method> entry : routes.entrySet()) {
            System.out.println(" - " + entry.getKey() + " → " + entry.getValue().getDeclaringClass().getSimpleName() + "." + entry.getValue().getName() + "()");
        }
    }

}
