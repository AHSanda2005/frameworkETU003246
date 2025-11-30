package com.example.web;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

import com.example.annotation.*;

import javax.servlet.ServletContext;

public class ControllerScanner {

    private static final Map<String, Map<String, Method>> routes = new HashMap<>();
    private static final Map<Method, Object> instances = new HashMap<>();

    public static final List<DynamicRouteEntry> dynamicRoutes = new ArrayList<>();


    public static void initialize(String basePackage, ServletContext context) {
        try {
            scanAndRegister(basePackage);

            context.setAttribute("routes", routes);
            context.setAttribute("instances", instances);
            context.setAttribute("dynamicRoutes", dynamicRoutes);

            System.out.println("Routes and dynamic routes stored in ServletContext.");
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
                continue;
            }

            if (!file.getName().endsWith(".class")) continue;

            String className = packageName + "." + file.getName().replace(".class", "");
            Class<?> cls = Class.forName(className);

            if (!cls.isAnnotationPresent(AnnotationController.class)) continue;

            AnnotationController ctrlAnno = cls.getAnnotation(AnnotationController.class);
            String baseRoute = ctrlAnno.value();

            Object instance = cls.getDeclaredConstructor().newInstance();
            boolean foundAnnotatedMethod = false;

            for (Method method : cls.getDeclaredMethods()) {

                String httpMethod = null;
                String routeValue = null;

                if (method.isAnnotationPresent(GetMethode.class)) {
                    httpMethod = "GET";
                    routeValue = method.getAnnotation(GetMethode.class).value();
                } else if (method.isAnnotationPresent(PostMethode.class)) {
                    httpMethod = "POST";
                    routeValue = method.getAnnotation(PostMethode.class).value();
                } else if (method.isAnnotationPresent(PutMethode.class)) {
                    httpMethod = "PUT";
                    routeValue = method.getAnnotation(PutMethode.class).value();
                } else if (method.isAnnotationPresent(DeleteMethode.class)) {
                    httpMethod = "DELETE";
                    routeValue = method.getAnnotation(DeleteMethode.class).value();
                } else if (method.isAnnotationPresent(PatchMethode.class)) {
                    httpMethod = "PATCH";
                    routeValue = method.getAnnotation(PatchMethode.class).value();
                } else {
                    // no HTTP annotation → skip
                    continue;
                }

                foundAnnotatedMethod = true;
                String fullRoute = baseRoute + routeValue;  


                if (fullRoute.contains("{")) {

                    String regex = fullRoute.replaceAll("\\{([^/]+)}", "(?<$1>[^/]+)");
                    regex = "^" + regex + "$";

                    DynamicRouteEntry entry = findOrCreateDynamicEntry(regex, instance);
                    entry.methods.put(httpMethod, method);
                    instances.put(method, instance);

                    System.out.println(" Registered dynamic route [" + httpMethod + "]: " + fullRoute + " → " + regex);
                }

                else {
                    routes.computeIfAbsent(fullRoute, k -> new HashMap<>())
                          .put(httpMethod, method);
                    instances.put(method, instance);

                    System.out.println(" Registered static route [" + httpMethod + "]: " + fullRoute);
                }
            }


            if (!foundAnnotatedMethod) {
                try {
                    Method defaultMethod = cls.getMethod("handle",
                            javax.servlet.http.HttpServletRequest.class,
                            javax.servlet.http.HttpServletResponse.class
                    );

                    routes.computeIfAbsent(baseRoute, k -> new HashMap<>())
                        .put("GET", defaultMethod);
                    instances.put(defaultMethod, instance);


                    System.out.println(" Default controller registered for: " + baseRoute);
                }
                catch (NoSuchMethodException ignored) {
                    System.out.println(" No @GetMethode and no handle() in " + cls.getSimpleName());
                }
            }
        }
    }

    private static DynamicRouteEntry findOrCreateDynamicEntry(String regex, Object instance) {
        for (DynamicRouteEntry e : dynamicRoutes) {
            if (e.pattern.pattern().equals(regex)) {
                return e;
            }
        }
        DynamicRouteEntry newEntry = new DynamicRouteEntry(Pattern.compile(regex), instance);
        dynamicRoutes.add(newEntry);
        return newEntry;
    }
    public static Method getStaticMethod(String path, String httpMethod) {
        Map<String, Method> methods = routes.get(path);
        if (methods == null) return null;
        return methods.get(httpMethod);
    }

    public static Object getController(Method method) {
        return instances.get(method);
    }

    public static List<DynamicRouteEntry> getDynamicRoutes() {
        return dynamicRoutes;
    }

    public static Set<String> getAllowedMethodsForPath(String path) {
        Map<String, Method> methods = routes.get(path);
        return methods == null ? Collections.emptySet() : methods.keySet();
    }

    // --------------------------------------------------------------------
    // DEBUGGING TOOLS
    // --------------------------------------------------------------------
    public static void printAllRoutes() {
        System.out.println("\n===== STATIC ROUTES =====");
        for (Map.Entry<String, Map<String, Method>> kv : routes.entrySet()) {
            String path = kv.getKey();
            String methodsList = String.join(", ", kv.getValue().keySet());
            System.out.println(" - " + path + " -> [" + methodsList + "]");
        }

        System.out.println("\n===== DYNAMIC ROUTES =====");
        for (DynamicRouteEntry e : dynamicRoutes) {
            String methodsList = String.join(", ", e.methods.keySet());
            System.out.println(" - " + e.pattern.pattern() + " -> [" + methodsList + "]");
        }
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
                    String annotation = m.isAnnotationPresent(com.example.annotation.GetMethode.class) ? " @GetMethode" : "";
                    System.out.println("   → " + m.getName() + annotation);
                }
            }
        }
    }
}

}
