package org.monke.connector.util;

/**
 * Utility class to get application version from Gradle / Maven.
 *
 * <p> Only works when running from JAR. When running from IDE, it will return default value.
 * Version is defined in {@code MANIFEST.MF} file under {@code Implementation-Version} attribute.
 *
 * <p> Note : A slightly more complicated method consists of defining a {@code version.properties} or {@code version.txt} file
 * in resources path, either statically or dynamically during the build process and reading it at runtime.
 */
public class Version {

    public static String getVersion() {
        try {
            return Version.class.getPackage().getImplementationVersion();

        } catch (Exception ex) {
            return "unknown";
        }
    }
}