package sandbox.java.net;

import sandbox.java.lang.String;

/**
 * This is a dummy class that implements just enough of {@link java.net.URLStreamHandlerFactory}
 * to allow us to compile {@link sandbox.java.net.URL}.
 */
@SuppressWarnings("unused")
public interface URLStreamHandlerFactory {
    URLStreamHandler createURLStreamHandler(String protocol);
}
