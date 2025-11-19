package com.example.web;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

public class RouteEntry {
    private Pattern pattern;
    private Method method;
    private Object instance;

    public RouteEntry(Pattern pattern, Method method, Object instance) {
        this.pattern = pattern;
        this.method = method;
        this.instance = instance;
    }

    public Pattern getPattern() { return pattern; }
    public Method getMethod() { return method; }
    public Object getInstance() { return instance; }
}
