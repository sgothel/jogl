/*
 * Copyright (c) 2020 Business Operation Systems GmbH. All Rights Reserved.
 */
package jogamp.graph.font.typecast.ot;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Utility to convert number of seconds since 12:00 midnight that started
 * January 1st 1904 in GMT/UTC time zone to a {@link Date} value.
 *
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 */
public class LongDateTime {
    
    private static final long BASE;

    static {
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone(ZoneId.of("UTC")));
        calendar.set(Calendar.YEAR, 1904);
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        BASE = calendar.getTimeInMillis();
    }

    /**
     * Converts a {@link LongDateTime} value to a {@link Date}.
     */
    public static Date toDate(long longDateTime) {
        return new Date(toSystemMillis(longDateTime));
    }

    /**
     * Converts a {@link LongDateTime} value to a Java system millis value compatible
     * with {@link System#currentTimeMillis()}.
     */
    public static long toSystemMillis(long longDateTime) {
        return BASE + 1000L * longDateTime;
    }
    
    /**
     * Converts a {@link Date} to a {@link LongDateTime} value.
     */
    public static long fromDate(Date date) {
        return fromSystemMillis(date.getTime());
    }
    
    /**
     * Converts a Java system millis value compatible
     * with {@link System#currentTimeMillis()} to a {@link LongDateTime} value.
     */
    public static long fromSystemMillis(long systemMillis) {
        return (systemMillis - BASE) / 1000L;
    }
    
}
