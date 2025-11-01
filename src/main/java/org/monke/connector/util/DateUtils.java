package org.monke.connector.util;

import java.time.Instant;

public class DateUtils {

    public static Instant getMostRecent(Instant i1, Instant i2) {
        return i1.compareTo(i2) > 0 ? i1 : i2;
    }
}
