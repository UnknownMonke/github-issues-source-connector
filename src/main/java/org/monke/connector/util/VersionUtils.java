package org.monke.connector.util;

/**
 * Utility class to get the version of the application from Gradle / Maven.
 *
 * <p> Only works when running from JAR. When running from IDE, it will return "0.0.0".
 */
public class VersionUtils {

    public static String getVersion() {
        try {
            return VersionUtils.class.getPackage().getImplementationVersion();

        } catch(Exception ex) {
            return "0.0.0";
        }
    }
}
