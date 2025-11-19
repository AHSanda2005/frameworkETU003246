package com.example.web;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

import com.example.annotation.AnnotationController;
import com.example.annotation.GetMethode;

import javax.servlet.ServletContext;

public class ControllerScanner {

    // STATIC ROUTES (exact match like "/test/hello")
    private static final Map<String, Method> routes = new HashMap<>();
    private static final Map<Method, Object> instances = new HashMap<>();

    // DYNAMIC ROUTES (regex match like "/test/bye/{id}")
    public static final List<RouteEntry> dynamicRoutes = new ArrayList<>();


    // --------------------------------------------------------------------
    // INITIALIZATION
    // --------------------------------------------------------------------
    public static void initialize(String basePackage, ServletContext context) {
        try {
            scanAndRegister(basePackage);

            // Store in ServletContext for FrontController access
            context.setAttribute("routes", routes);
            context.setAttribute("instances", instances);
            context.setAttribute("dynamicRoutes", dynamicRoutes);

            System.out.println("✅ Routes and dynamic routes stored in ServletContext.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // --------------------------------------------------------------------
    // SCAN CONTROLLERS AND REGISTER ROUTES
    // --------------------------------------------------------------------
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


            // ---------------------------
            // REGISTER @GetMethode METHODS
            // ---------------------------
            for (Method method : cls.getDeclaredMethods()) {

                if (!method.isAnnotationPresent(GetMethode.class))
                    continue;

                foundAnnotatedMethod = true;

                GetMethode getAnnotation = method.getAnnotation(GetMethode.class);
                String fullRoute = baseRoute + getAnnotation.value();  // e.g. "/test/bye/{id}"


                // Check if it's dynamic (contains {...})
                if (fullRoute.contains("{")) {

                    // Convert "/test/bye/{id}" → ^/test/bye/(?<id>[^/]+)$
                    String regex = fullRoute.replaceAll("\\{([^/]+)}", "(?<$1>[^/]+)");
                    regex = "^" + regex + "$";

                    dynamicRoutes.add(new RouteEntry(Pattern.compile(regex), method, instance));

                    System.out.println("🔵 Registered dynamic route: " + fullRoute + " → " + regex);
                }
                else {
                    // Static route
                    routes.put(fullRoute, method);
                    instances.put(method, instance);

                    System.out.println("🟢 Registered static route: " + fullRoute);
                }
            }


            // --------------------------------------------------------
            // FALLBACK TO handle() IF NO @GetMethode IS FOUND
            // --------------------------------------------------------
            if (!foundAnnotatedMethod) {
                try {
                    Method defaultMethod = cls.getMethod("handle",
                            javax.servlet.http.HttpServletRequest.class,
                            javax.servlet.http.HttpServletResponse.class
                    );

                    routes.put(baseRoute, defaultMethod);
                    instances.put(defaultMethod, instance);

                    System.out.println("⚪ Default controller registered for: " + baseRoute);
                }
                catch (NoSuchMethodException ignored) {
                    System.out.println("❌ No @GetMethode and no handle() in " + cls.getSimpleName());
                }
            }
        }
    }


    // --------------------------------------------------------------------
    // GETTERS
    // --------------------------------------------------------------------
    public static Method getStaticMethod(String path) {
        return routes.get(path);
    }

    public static Object getController(Method method) {
        return instances.get(method);
    }

    public static List<RouteEntry> getDynamicRoutes() {
        return dynamicRoutes;
    }


    // --------------------------------------------------------------------
    // DEBUGGING TOOLS
    // --------------------------------------------------------------------
    public static void printAllRoutes() {
        System.out.println("\n===== STATIC ROUTES =====");
        for (String r : routes.keySet()) {
            System.out.println(" - " + r);
        }

        System.out.println("\n===== DYNAMIC ROUTES =====");
        for (RouteEntry e : dynamicRoutes) {
            System.out.println(" - " + e.getPattern().pattern());
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
