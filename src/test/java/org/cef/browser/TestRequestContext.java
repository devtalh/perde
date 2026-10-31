package org.cef.browser;

import org.cef.handler.CefRequestContextHandler;
import java.util.HashMap;
import java.util.Map;

/** Native-free test double in the API package to access its package-private constructor. */
public final class TestRequestContext extends CefRequestContext {
        public final Map<String, Object> preferences = new HashMap<>();
        public boolean available = true, writable = true, verify = true;
        public String error;
        @Override public void dispose() { }
        @Override public boolean isGlobal() { return false; }
        @Override public CefRequestContextHandler getHandler() { return null; }
        @Override public boolean hasPreference(String name) { return available; }
        @Override public boolean canSetPreference(String name) { return writable; }
        @Override public Object getPreference(String name) { return verify ? preferences.get(name) : 1; }
        @Override public Map<String, Object> getAllPreferences(boolean defaults) { return preferences; }
        @Override public String setPreference(String name, Object value) { preferences.put(name, value); return error; }
    }
