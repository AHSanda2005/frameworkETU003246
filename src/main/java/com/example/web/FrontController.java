package com.example.web;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.*;
import javax.servlet.http.*;

public class FrontController extends HttpServlet {

    @Override
public void init() throws ServletException {
    ControllerScanner.initialize("com.example.controller", getServletContext());

    ControllerScanner.printAllRoutes();

    ControllerScanner.listMethods("com.example.controller");
}

    @Override
protected void service(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

    response.setContentType("text/html;charset=UTF-8");

    Map<String, Method> routes = (Map<String, Method>) getServletContext().getAttribute("routes");
    Map<Method, Object> instances = (Map<Method, Object>) getServletContext().getAttribute("instances");

    if (routes == null || instances == null) {
        throw new ServletException("❌ Routes not initialized in ServletContext");
    }

    String path = request.getRequestURI().substring(request.getContextPath().length());
    if (path.isEmpty() || path.equals("/")) path = "/index";

    Method method = routes.get(path);
    if (method != null) {
        Object controller = instances.get(method);

        try (PrintWriter out = response.getWriter()) {
            Class<?> cls = controller.getClass();
            out.println("<html><body>");
            out.println("<h2>Controller: " + cls.getSimpleName() + "</h2>");
            out.println("<h3>Methods:</h3>");
            out.println("<ul>");

            for (Method m : cls.getDeclaredMethods()) {
                String annotation = m.isAnnotationPresent(com.example.annotation.GetMethode.class) ? " @GetMethode" : "";
                out.println("<li>" + m.getName() + "()" + annotation + "</li>");
            }

            out.println("</ul>");

            out.println("<h3>Executing method for this URL:</h3>");
            out.println("<div style='border:1px solid #ccc;padding:10px;'>");
            method.invoke(controller, request, response);
            out.println("</div>");

            out.println("</body></html>");
        } catch (Exception e) {
            throw new ServletException("Failed to list methods or invoke controller", e);
        }
        return;
    }

    handleFileRequest(request, response, path);
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
