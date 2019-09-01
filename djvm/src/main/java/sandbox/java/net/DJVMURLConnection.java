package sandbox.java.net;

import sandbox.java.io.InputStream;
import sandbox.java.lang.DJVM;
import sandbox.java.lang.String;
import sandbox.java.util.ArrayList;
import sandbox.java.util.LinkedHashMap;
import sandbox.java.util.List;
import sandbox.java.util.Map;

import java.io.IOException;

import static sandbox.java.util.Collections.*;

@SuppressWarnings({"unused", "RedundantThrows"})
final class DJVMURLConnection extends URLConnection {
    private static final java.lang.String STREAMING_ONLY = "Only streaming is supported";

    private final java.net.URLConnection connection;

    DJVMURLConnection(URL url, java.net.URLConnection connection) {
        super(url);
        this.connection = connection;

        connection.setUseCaches(false);
        connection.setAllowUserInteraction(false);
        connection.setIfModifiedSince(0);
    }

    @Override
    public void connect() throws IOException {
        connection.connect();
        connected = true;
    }

    @Override
    public void setConnectTimeout(int timeout) {
        connection.setConnectTimeout(timeout);
    }

    @Override
    public int getConnectTimeout() {
        return connection.getConnectTimeout();
    }

    @Override
    public void setReadTimeout(int timeout) {
        connection.setReadTimeout(timeout);
    }

    @Override
    public int getReadTimeout() {
        return connection.getReadTimeout();
    }

    @Override
    public String getHeaderField(String name) {
        return String.toDJVM(connection.getHeaderField(String.fromDJVM(name)));
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        return toDJVM(connection.getHeaderFields());
    }

    @Override
    public String getHeaderFieldKey(int n) {
        return String.toDJVM(connection.getHeaderFieldKey(n));
    }

    @Override
    public String getHeaderField(int n) {
        return String.toDJVM(connection.getHeaderField(n));
    }

    @Override
    public void setDoInput(boolean doInput) {
        connection.setDoInput(doInput);
    }

    @Override
    public boolean getDoInput() {
        return connection.getDoInput();
    }

    @Override
    public boolean getDoOutput() {
        return connection.getDoOutput();
    }

    @Override
    public boolean getAllowUserInteraction() {
        return connection.getAllowUserInteraction();
    }

    @Override
    public long getIfModifiedSince() {
        return connection.getIfModifiedSince();
    }

    @Override
    public void setRequestProperty(String key, String value)  {
        connection.setRequestProperty(String.fromDJVM(key), String.fromDJVM(value));
    }

    @Override
    public void addRequestProperty(String key, String value) {
        connection.addRequestProperty(String.fromDJVM(key), String.fromDJVM(value));
    }

    @Override
    public String getRequestProperty(String key) {
        return String.toDJVM(connection.getRequestProperty(String.fromDJVM(key)));
    }

    @Override
    public Map<String, List<String>> getRequestProperties() {
        return toDJVM(connection.getRequestProperties());
    }

    @Override
    public Object getContent() throws IOException {
        throw new UnsupportedOperationException(STREAMING_ONLY);
    }

    @Override
    public Object getContent(Class[] classes) throws IOException {
        throw new UnsupportedOperationException(STREAMING_ONLY);
    }

    @Override
    public final InputStream getInputStream() throws IOException {
        try {
            return InputStream.toDJVM(connection.getInputStream());
        } catch (IOException e) {
            throw (IOException) DJVM.fromDJVM(DJVM.doCatch(e));
        }
    }

    private static Map<String, List<String>> toDJVM(java.util.Map<java.lang.String, java.util.List<java.lang.String>> map) {
        if (map.isEmpty()) {
            return emptyMap();
        }

        Map<String, List<String>> sandyMap = new LinkedHashMap<>();
        for (java.util.Map.Entry<java.lang.String, java.util.List<java.lang.String>> entry : map.entrySet()) {
            sandyMap.put(String.toDJVM(entry.getKey()), toDJVM(entry.getValue()));
        }
        return unmodifiableMap(sandyMap);
    }

    private static List<String> toDJVM(java.util.List<java.lang.String> list) {
        if (list.isEmpty()) {
            return emptyList();
        }

        List<String> sandyList = new ArrayList<>(list.size());
        for (java.lang.String item : list) {
            sandyList.add(String.toDJVM(item));
        }
        return unmodifiableList(sandyList);
    }
}
