package com.example.web;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class DynamicRouteEntry {
    public Pattern pattern;
    public Map<String, Method> methods = new HashMap<>();
    public Object instance;

    public DynamicRouteEntry(Pattern pattern, Object instance) {
        this.pattern = pattern;
        this.instance = instance;
    }
}
