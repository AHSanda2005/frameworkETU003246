package com.example.web;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.*;
import javax.servlet.http.*;

import com.example.util.ModelView;

public class FrontController extends HttpServlet {

    @Override
    public void init() throws ServletException {
        ControllerScanner.initialize("com.example.controller", getServletContext());

        ControllerScanner.printAllRoutes();
    }
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        service(request, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        service(request, response);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        Map<String, Map<String, Method>> routes =
                (Map<String, Map<String, Method>>) getServletContext().getAttribute("routes");

        Map<Method, Object> instances =
                (Map<Method, Object>) getServletContext().getAttribute("instances");

        List<DynamicRouteEntry> dynamicRoutes =
                (List<DynamicRouteEntry>) getServletContext().getAttribute("dynamicRoutes");

        if (routes == null || instances == null || dynamicRoutes == null) {
            throw new ServletException("Routes not initialized in ServletContext");
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.isEmpty() || path.equals("/")) path = "/index";

        String httpMethod = request.getMethod();

        if ("POST".equalsIgnoreCase(httpMethod)) {
            String override = request.getParameter("_method");
            if (override != null && !override.isEmpty()) {
                httpMethod = override.toUpperCase();
            }
        }


        DynamicRouteEntry matched = null;
        java.util.regex.Matcher matcher = null;
        boolean pathExists = false;

        for (DynamicRouteEntry entry : dynamicRoutes) {
            matcher = entry.pattern.matcher(path);
            if (matcher.matches()) {
                pathExists = true; 
                if (entry.methods.containsKey(httpMethod)) {
                    matched = entry;
                    break; 
                }
            }
        }

        if (matched != null) {
            invokeDynamicRoute(matched, matcher, request, response, httpMethod);
            return;
        } else if (pathExists) {
            response.setStatus(405);
            response.getWriter().println("HTTP 405: Method Not Allowed");
            return;
        }

        Map<String, Method> httpMethods = routes.get(path);

        if (httpMethods != null) {
            Method method = httpMethods.get(httpMethod);

            if (method == null) {
                response.setStatus(405);
                response.getWriter().println("HTTP 405: Method Not Allowed");
                return;
            }

            Object controller = instances.get(method);
            invokeMethod(controller, method, request, response);
            return;
        }
        handleFileRequest(request, response, path);
    }

    private void invokeDynamicRoute(DynamicRouteEntry matched, java.util.regex.Matcher matcher,
                                    HttpServletRequest request, HttpServletResponse response,
                                    String httpMethod)
            throws ServletException, IOException {

        Method method = matched.methods.get(httpMethod);
        Object controller = matched.instance;

        if (method == null) {
            response.setStatus(405);
            response.getWriter().println("HTTP 405: Method Not Allowed");
            return;
        }

        java.util.regex.Pattern groupPattern =
                java.util.regex.Pattern.compile("\\(\\?<([a-zA-Z0-9_]+)>");

        java.util.regex.Matcher gm = groupPattern.matcher(matched.pattern.pattern());

        while (gm.find()) {
            String name = gm.group(1);
            String value = matcher.group(name);
            if (value != null) {
                request.setAttribute(name, value); 
            }
        }

        invokeMethod(controller, method, request, response);
    }

    private void invokeMethod(Object controller, Method method,
                            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try (PrintWriter out = response.getWriter()) {

            Object[] args = buildMethodArguments(method, request, response);

            Object result = method.invoke(controller, args);

            if (result instanceof ModelView) {
        ModelView mv = (ModelView) result;

        for (Map.Entry<String, Object> entry : mv.getData().entrySet()) {
            request.setAttribute(entry.getKey(), entry.getValue());
        }

        String view = mv.getView();
        if (!view.startsWith("/views/")) view = "/views/" + view;
        request.getRequestDispatcher(view).forward(request, response);

    } else if (result instanceof String) {
        out.println((String) result);
    }


        } catch (Exception e) {
            throw new ServletException("Failed to invoke controller method", e);
        }
    }

    private Object[] buildMethodArguments(Method method,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {

    Class<?>[] paramTypes = method.getParameterTypes();
    java.lang.annotation.Annotation[][] paramAnnotations = method.getParameterAnnotations();

    Object[] args = new Object[paramTypes.length];

    for (int i = 0; i < paramTypes.length; i++) {

        if (paramTypes[i] == HttpServletRequest.class) {
            args[i] = request;
            continue;
        }

        if (paramTypes[i] == HttpServletResponse.class) {
            args[i] = response;
            continue;
        }

        Object value = null;

        String paramName = method.getParameters()[i].getName();
        value = request.getAttribute(paramName);

        if ((value == null || value.toString().isEmpty()) && paramAnnotations[i].length > 0) {
            for (java.lang.annotation.Annotation a : paramAnnotations[i]) {
                if (a instanceof com.example.annotation.RequestParam) {
                    com.example.annotation.RequestParam rp =
                            (com.example.annotation.RequestParam) a;
                    paramName = rp.value();
                    value = request.getParameter(paramName);
                    break;
                }
            }
        }

        if (value == null) {
            value = request.getParameter(paramName);
        }

        if (value == null) {
            args[i] = null; 
        } else {
            args[i] = convertType(value.toString(), paramTypes[i]);
        }
    }

    return args;
}

    private Object convertType(String value, Class<?> type) {
        if (value == null) return null;

        try {
            if (type == int.class || type == Integer.class) return Integer.parseInt(value);
            if (type == double.class || type == Double.class) return Double.parseDouble(value);
            if (type == float.class  || type == Float.class)  return Float.parseFloat(value);
            if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);

        } catch (Exception e) {
            return null;
        }

        return value; 
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
