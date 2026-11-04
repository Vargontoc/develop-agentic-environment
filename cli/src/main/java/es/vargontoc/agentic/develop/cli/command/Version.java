package es.vargontoc.agentic.develop.cli.command;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Versión de la CLI, tomada del {@code pom.xml} a través de un recurso filtrado por Maven. */
public final class Version {

    private static final String RESOURCE = "/cli-version.properties";
    private static final String UNKNOWN = "unknown";

    private Version() {}

    public static String current() {
        try(InputStream in = Version.class.getResourceAsStream(RESOURCE)) {
            if(in == null)
                return UNKNOWN;
            Properties properties = new Properties();
            properties.load(in);
            String version = properties.getProperty("version", UNKNOWN);
            return version.startsWith("${") ? UNKNOWN : version;
        } catch(IOException e) {
            return UNKNOWN;
        }
    }
}
