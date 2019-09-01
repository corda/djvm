package sandbox.java.net;

import org.jetbrains.annotations.NotNull;
import sandbox.java.io.InputStream;
import sandbox.java.lang.DJVM;
import sandbox.java.lang.String;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;

@SuppressWarnings({"unused", "RedundantThrows", "WeakerAccess"})
public final class URL extends sandbox.java.lang.Object implements Serializable {
    private static final java.lang.String HANDLER_UNSUPPORTED = "URLStreamHandler operations unsupported";

    private final java.net.URL url;
    private final String externalForm;

    private URL(java.net.URL url) {
        this.url = url;
        externalForm = String.toDJVM(url.toExternalForm());
    }

    public URL(String protocol, String host, int port, String file) throws IOException {
        try {
            url = new java.net.URL(
                DJVM.validateProtocol(String.fromDJVM(protocol)),
                String.fromDJVM(host),
                port,
                String.fromDJVM(file)
            );
            externalForm = String.toDJVM(url.toExternalForm());
        } catch (MalformedURLException e) {
            throw (IOException) DJVM.fromDJVM(DJVM.doCatch(e));
        }
    }

    public URL(String protocol, String host, String file) throws IOException {
        try {
            url = new java.net.URL(
                DJVM.validateProtocol(String.fromDJVM(protocol)),
                String.fromDJVM(host),
                String.fromDJVM(file)
            );
            externalForm = String.toDJVM(url.toExternalForm());
        } catch (MalformedURLException e) {
            throw (IOException) DJVM.fromDJVM(DJVM.doCatch(e));
        }
    }

    public URL(String protocol, String host, int port, String file, URLStreamHandler handler) throws IOException {
        throw new UnsupportedOperationException(HANDLER_UNSUPPORTED);
    }

    public URL(String spec) throws IOException {
        try {
            url = new java.net.URL(String.fromDJVM(spec));
            DJVM.validateProtocol(url.getProtocol());
            externalForm = String.toDJVM(url.toExternalForm());
        } catch (MalformedURLException e) {
            throw (IOException) DJVM.fromDJVM(DJVM.doCatch(e));
        }
    }

    public URL(URL context, String spec) throws IOException {
        try {
            url = new java.net.URL(fromDJVM(context), String.fromDJVM(spec));
            DJVM.validateProtocol(url.getProtocol());
            externalForm = String.toDJVM(url.toExternalForm());
        } catch (MalformedURLException e) {
            throw (IOException) DJVM.fromDJVM(DJVM.doCatch(e));
        }
    }

    public URL(URL context, String spec, URLStreamHandler handler) throws IOException {
        throw new UnsupportedOperationException(HANDLER_UNSUPPORTED);
    }

    public String getQuery() {
        return String.toDJVM(url.getQuery());
    }

    public String getPath() {
        return String.toDJVM(url.getPath());
    }

    public String getUserInfo() {
        return String.toDJVM(url.getUserInfo());
    }

    public String getAuthority() {
        return String.toDJVM(url.getAuthority());
    }

    public int getPort() {
        return url.getPort();
    }

    public int getDefaultPort() {
        return url.getDefaultPort();
    }

    public String getProtocol() {
        return String.toDJVM(url.getProtocol());
    }

    public String getHost() {
        return String.toDJVM(url.getHost());
    }

    public String getFile() {
        return String.toDJVM(url.getFile());
    }

    public String getRef() {
        return String.toDJVM(url.getRef());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof URL)) {
            return false;
        }
        return externalForm.equals(((URL) obj).externalForm);
    }

    @Override
    public int hashCode() {
        return externalForm.hashCode();
    }

    @Override
    @NotNull
    public String toDJVMString() {
        return externalForm;
    }

    public String toExternalForm() {
        return externalForm;
    }

    public boolean sameFile(URL other) {
        return url.sameFile(fromDJVM(other));
    }

    public URI toURI() throws Exception {
        return new URI(toString());
    }

    public URLConnection openConnection() throws IOException {
        try {
            return new DJVMURLConnection(this, url.openConnection());
        } catch (IOException e) {
            throw (IOException) DJVM.fromDJVM(DJVM.doCatch(e));
        }
    }

    public final InputStream openStream() throws IOException {
        return openConnection().getInputStream();
    }

    public final Object getContent() throws IOException {
        return openConnection().getContent();
    }

    public final Object getContent(Class[] classes) throws IOException {
        return openConnection().getContent(classes);
    }

    @Override
    @NotNull
    protected java.net.URL fromDJVM() {
        return url;
    }

    void set(String protocol, String host, int port, String file, String ref) {
    }

    void set(String protocol, String host, int port,
             String authority, String userInfo, String path,
             String query, String ref) {
    }

    public static void setURLStreamHandlerFactory(URLStreamHandlerFactory factory) {
        throw new UnsupportedOperationException(HANDLER_UNSUPPORTED);
    }

    public static java.net.URL fromDJVM(URL value) {
        return (value == null) ? null : value.fromDJVM();
    }

    public static URL toDJVM(java.net.URL value) {
        return (value == null) ? null : new URL(value);
    }
}
