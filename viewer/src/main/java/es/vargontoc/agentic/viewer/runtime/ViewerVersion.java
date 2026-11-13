package es.vargontoc.agentic.viewer.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Versión del visor, tomada del {@code pom.xml} a través de un recurso filtrado por Maven. */
public final class ViewerVersion {

    private static final String RESOURCE = "/viewer-version.properties";
    private static final String UNKNOWN = "desconocida";

    private ViewerVersion() {}

    public static String current() {
        try (InputStream in = ViewerVersion.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return UNKNOWN;
            }
            Properties properties = new Properties();
            properties.load(in);
            String version = properties.getProperty("version", UNKNOWN);
            return version.startsWith("${") ? UNKNOWN : version;
        } catch (IOException exception) {
            return UNKNOWN;
        }
    }
}
