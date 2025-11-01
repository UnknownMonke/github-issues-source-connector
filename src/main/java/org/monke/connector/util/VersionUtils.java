package org.monke.connector.util;

/**
 * <p>Utility class to get the version of the application from Gradle / Maven.</p>
 * <p>Only works when running from JAR.</p>
 * <p>When running from IDE, it will return "0.0.0".</p>
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
