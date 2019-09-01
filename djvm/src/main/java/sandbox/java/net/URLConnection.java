package sandbox.java.net;

import sandbox.java.io.InputStream;
import sandbox.java.lang.String;
import sandbox.java.util.List;
import sandbox.java.util.Map;

import java.io.IOException;

/**
 * This is a dummy class that implements just enough of {@link java.net.URLConnection}
 * to allow us to compile {@link sandbox.java.net.DJVMURLConnection}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class URLConnection extends sandbox.java.lang.Object {
    private static final java.lang.String NOT_IMPLEMENTED = "Dummy class - not implemented";

    protected URL url;
    protected boolean connected = false;

    private int connectTimeout;
    private int readTimeout;

    protected URLConnection(URL url) {
        this.url = url;
    }

    public URL getURL() {
        return url;
    }

    public abstract void connect() throws IOException;

    public void setConnectTimeout(int timeout) {
        this.connectTimeout = timeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setReadTimeout(int timeout) {
        this.readTimeout = timeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public String getHeaderField(String name) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public Map<String, List<String>> getHeaderFields() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public String getHeaderFieldKey(int n) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public String getHeaderField(int n) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public Object getContent() throws IOException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public Object getContent(Class[] classes) throws IOException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public void setDoInput(boolean doinput) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public boolean getDoInput() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public boolean getDoOutput() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public boolean getAllowUserInteraction() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public boolean getUseCaches() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public long getIfModifiedSince() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public boolean getDefaultUseCaches() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public void setRequestProperty(String key, String value) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public void addRequestProperty(String key, String value) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public String getRequestProperty(String key) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public Map<String,List<String>> getRequestProperties() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public InputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }
}
