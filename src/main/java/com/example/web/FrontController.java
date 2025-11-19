package com.example.web;

import com.example.util.*;

import java.io.*;
import java.lang.reflect.Method;
import java.util.List;
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

    List<RouteEntry> dynamicRoutes =
        (List<RouteEntry>) getServletContext().getAttribute("dynamicRoutes");


    String path = request.getRequestURI().substring(request.getContextPath().length());
    if (path.isEmpty() || path.equals("/")) path = "/index";

    RouteEntry matched = null;
java.util.regex.Matcher matcher = null;

for (RouteEntry entry : dynamicRoutes) {
    matcher = entry.getPattern().matcher(path);
    if (matcher.matches()) {
        matched = entry;
        break;
    }
}

if (matched != null) {

    Method method = matched.getMethod();
    Object controller = matched.getInstance();

    for (String groupName : matched.getPattern().pattern().split("\\(\\?<")) {
        if (groupName.contains(">")) {
            String name = groupName.substring(0, groupName.indexOf(">"));
            String value = matcher.group(name);
            if (value != null) {
                request.setAttribute(name, value);
            }
        }
    }


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
            Object result = method.invoke(controller, request, response);

            if (result != null && result instanceof ModelView) {
                ModelView mv = (ModelView) result;
                for (Map.Entry<String, Object> entry : mv.getData().entrySet()) {
                    request.setAttribute(entry.getKey(), entry.getValue());
                }

                String view = mv.getView(); 
                if (!view.startsWith("/views/")) {
                    view = "/views/" + view;
                }
                RequestDispatcher rd = request.getRequestDispatcher(view);
                rd.forward(request, response);
                return; 
            }

            if (result != null && result instanceof String) {
                out.println(result.toString());
            }
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
