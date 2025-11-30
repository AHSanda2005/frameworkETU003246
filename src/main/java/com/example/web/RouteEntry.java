package com.example.web;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

public class RouteEntry {
    private String httpMethod;   
    private String route; 
    private Pattern pattern;
    private Method method;
    private Object instance;

    public RouteEntry(String httpMethod, String route, Pattern pattern, Method method, Object instance) {
        this.httpMethod = httpMethod;
        this.route = route;
        this.pattern = pattern;
        this.method = method;
        this.instance = instance;
    }

    public String getHttpMethod() { return httpMethod; }
    public String getRoute() { return route; }
    public Pattern getPattern() { return pattern; }
    public Method getMethod() { return method; }
    public Object getInstance() { return instance; }
}
