package com.example.web;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FrontController extends HttpServlet {

    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        String url = requestURI.substring(contextPath.length());

        String viewName = null;
        /*if ("/".equals(url)) {
            viewName = "home.jsp";
        } else if ("/hello".equals(url)) {
            viewName = "hello.jsp";
        } else if ("/test-url".equals(url)) {
            viewName = "test.jsp";
        }*/

        if (viewName != null) {
            RequestDispatcher dispatcher = request.getRequestDispatcher("/views/" + viewName);
            dispatcher.forward(request, response);
        } else {
            response.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = response.getWriter()) {
                out.println("<h2>url inconnu pour cette url : " + url + "</h2>");
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        service(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        service(request, response);
    }

    @Override
    public String getServletInfo() {
        return "FrontServlet for the custom framework.";
    }
}