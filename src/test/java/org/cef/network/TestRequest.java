package org.cef.network;

import org.cef.network.CefRequest;
import org.cef.network.CefPostData;
import java.util.Map;

/** Native-free request double for exercising the real JCEF callbacks. */
public final class TestRequest extends CefRequest {
    private final String url;
    public TestRequest(String url) { this.url = url; }
    @Override public void dispose() {  }
    @Override public long getIdentifier() { return 0; }
    @Override public boolean isReadOnly() { return false; }
    @Override public String getURL() { return url; }
    @Override public void setURL(String url) {  }
    @Override public String getMethod() { return ""; }
    @Override public void setMethod(String method) {  }
    @Override public void setReferrer(String url, ReferrerPolicy policy) {  }
    @Override public String getReferrerURL() { return ""; }
    @Override public ReferrerPolicy getReferrerPolicy() { return null; }
    @Override public CefPostData getPostData() { return null; }
    @Override public void setPostData(CefPostData postData) {  }
    @Override public String getHeaderByName(String name) { return ""; }
    @Override public void setHeaderByName(String name, String value, boolean overwrite) {  }
    @Override public void getHeaderMap(Map<String, String> headerMap) {  }
    @Override public void setHeaderMap(Map<String, String> headerMap) {  }
    @Override public void set(  String url, String method, CefPostData postData, Map<String, String> headerMap) {  }
    @Override public int getFlags() { return 0; }
    @Override public void setFlags(int flags) {  }
    @Override public String getFirstPartyForCookies() { return ""; }
    @Override public void setFirstPartyForCookies(String url) {  }
    @Override public ResourceType getResourceType() { return ResourceType.RT_XHR; }
    @Override public TransitionType getTransitionType() { return null; }
}
