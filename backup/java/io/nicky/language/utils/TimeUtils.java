package language.utils;

import java.lang.management.ManagementFactory;
import java.util.Locale;

public final class TimeUtils {

    public static final double MILLISECOND = 1_000;
    public static final double NANOSECOND = 1_000_000;

    private TimeUtils() { }

    public static String toSeconds(long ms) {
        return format(ms / MILLISECOND) + "s";
    }

    public static String toMilliseconds(long nano) {
        return format(nano / NANOSECOND) + "ms";
    }

    public static String format(double time) {
        return String.format(Locale.US, "%.2f", time);
    }

    public static double getUptime(long uptime) {
        return (System.currentTimeMillis() - uptime) / MILLISECOND;
    }

    public static long getJVMUptime() {
        return System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getStartTime();
    }

}
