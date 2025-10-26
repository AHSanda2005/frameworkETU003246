package com.example.web;

import java.io.*;
import java.util.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.servlet.*;
import javax.servlet.http.*;

import com.example.annotation.AnnotationController;
import com.example.annotation.GetMethode;

public class FrontController extends HttpServlet {

    private final Map<String, Class<?>> routeMap = new HashMap<>();

    @Override
    public void init() throws ServletException {
        super.init();

        String basePackage = "com.example.controller";
        Set<Class<?>> controllers = findClassesWithAnnotation(basePackage, AnnotationController.class);

        for (Class<?> controller : controllers) {
            AnnotationController ac = controller.getAnnotation(AnnotationController.class);
            String prefix = ac.value();
            routeMap.put(prefix, controller);
            System.out.println("Mapped route: " + prefix + " -> " + controller.getName());
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.isEmpty() || path.equals("/")) path = "/index";

        boolean handled = false;

        for (Map.Entry<String, Class<?>> entry : routeMap.entrySet()) {
            String prefix = entry.getKey();
            Class<?> controllerClass = entry.getValue();

            if (path.startsWith(prefix)) {
                try {
                    Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();

                    for (Method method : controllerClass.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(GetMethode.class)) {
                            GetMethode gm = method.getAnnotation(GetMethode.class);
                            String fullPath = prefix + gm.value();

                            String normalizedPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
                            String normalizedFullPath = fullPath.endsWith("/") ? fullPath.substring(0, fullPath.length() - 1) : fullPath;

                            if (normalizedPath.equals(normalizedFullPath)) {
                                method.invoke(controllerInstance, request, response);
                                handled = true;
                                return;
                            }
                        }
                    }

                    if (!handled) {
                        try {
                            controllerClass.getMethod("handle", HttpServletRequest.class, HttpServletResponse.class)
                                           .invoke(controllerInstance, request, response);
                            handled = true;
                            return;
                        } catch (NoSuchMethodException ignored) {}
                    }

                } catch (Exception e) {
                    throw new ServletException("Error invoking controller for path " + path, e);
                }
            }
        }

        if (!handled) handleFileRequest(request, response, path);
    }

    private Set<Class<?>> findClassesWithAnnotation(String basePackage, Class<?> annotation) {
        Set<Class<?>> classes = new HashSet<>();
        try {
            String path = basePackage.replace('.', '/');
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String filePath = URLDecoder.decode(resource.getFile(), "UTF-8");
                File dir = new File(filePath);

                if (dir.exists()) {
                    for (File file : Objects.requireNonNull(dir.listFiles())) {
                        if (file.getName().endsWith(".class")) {
                            String className = basePackage + '.' + file.getName().replace(".class", "");
                            try {
                                Class<?> cls = Class.forName(className);
                                if (cls.isAnnotationPresent((Class) annotation)) {
                                    classes.add(cls);
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                } else if (filePath.contains(".jar!")) {
                    String jarPath = filePath.substring(5, filePath.indexOf("!"));
                    try (JarFile jar = new JarFile(jarPath)) {
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            if (name.startsWith(path) && name.endsWith(".class")) {
                                String className = name.replace('/', '.').replace(".class", "");
                                try {
                                    Class<?> cls = Class.forName(className);
                                    if (cls.isAnnotationPresent((Class) annotation)) {
                                        classes.add(cls);
                                    }
                                } catch (Throwable ignored) {}
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classes;
    }

    private void handleFileRequest(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException, ServletException {
        if (path.startsWith("/")) path = path.substring(1);
        if (path.isEmpty()) path = "index.html";

        if (!path.endsWith(".html") && !path.endsWith(".jsp")) {
            if (fileExists(request, "/views/" + path + ".html")) path += ".html";
            else if (fileExists(request, "/views/" + path + ".jsp")) path += ".jsp";
        }

        String fullPath = "/views/" + path;

        if (fileExists(request, fullPath)) {
            if (path.endsWith(".jsp"))
                request.getRequestDispatcher(fullPath).forward(request, response);
            else
                serveHtml(request, response, fullPath);
        } else {
            try (PrintWriter out = response.getWriter()) {
                out.println("<h2>Requested resource not found for URL: " + path + "</h2>");
            }
        }
    }

    private boolean fileExists(HttpServletRequest request, String relativePath) {
        String realPath = getServletContext().getRealPath(relativePath);
        return realPath != null && new File(realPath).exists();
    }

    private void serveHtml(HttpServletRequest request, HttpServletResponse response, String relativePath)
            throws IOException {
        File file = new File(getServletContext().getRealPath(relativePath));
        try (BufferedReader reader = new BufferedReader(new FileReader(file));
             PrintWriter out = response.getWriter()) {
            String line;
            while ((line = reader.readLine()) != null) out.println(line);
        }
    }
}