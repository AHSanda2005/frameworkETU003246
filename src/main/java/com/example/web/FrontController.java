package com.example.web;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class FrontController extends HttpServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestURI.substring(contextPath.length());

        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        if (path.isEmpty()) {
            path = "index.html";
        }

        if (!path.endsWith(".html") && !path.endsWith(".jsp")) {
            if (fileExists(request, "/views/" + path + ".html")) {
                path = path + ".html";
            } else if (fileExists(request, "/views/" + path + ".jsp")) {
                path = path + ".jsp";
            }
        }

        String fullPath = "/views/" + path;

        if (fileExists(request, fullPath)) {
            if (path.endsWith(".jsp")) {
                RequestDispatcher dispatcher = request.getRequestDispatcher(fullPath);
                dispatcher.forward(request, response);
            } else if (path.endsWith(".html")) {
                serveHtml(request, response, fullPath);
            }
        } else {
            try (PrintWriter out = response.getWriter()) {
                out.println("<h2>Requested resource not found for URL: " + requestURI + "</h2>");
            }
        }
    }

    private boolean fileExists(HttpServletRequest request, String relativePath) {
        String realPath = getServletContext().getRealPath(relativePath);
        if (realPath == null) return false;
        File file = new File(realPath);
        return file.exists() && file.isFile();
    }

    private void serveHtml(HttpServletRequest request, HttpServletResponse response, String relativePath)
            throws IOException {
        String realPath = getServletContext().getRealPath(relativePath);
        File file = new File(realPath);

        response.setContentType("text/html;charset=UTF-8");

        try (BufferedReader reader = new BufferedReader(new FileReader(file));
             PrintWriter out = response.getWriter()) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.println(line);
            }
        }
    }

    @Override
    public String getServletInfo() {
        return "FrontController that dynamically loads JSP and HTML files from /views";
    }
}
