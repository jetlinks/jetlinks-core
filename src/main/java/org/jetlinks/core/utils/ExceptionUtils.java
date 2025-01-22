package org.jetlinks.core.utils;

public class ExceptionUtils {

    private static final boolean compactEnabled =
        Boolean.parseBoolean(
            System.getProperty("jetlinks.exception.compact.enabled", "true")
        );

    // 无关紧要的异常栈信息
    private static final String[] unimportantPackages = {
        "reactor.core.publisher",
        "reactor.core.scheduler",
        "reactor.netty",
        "io.netty.channel",
        "io.netty.handler",
        "io.netty.util.internal",
        "java.util.concurrent.FutureTask",
        "java.util.concurrent.ThreadPoolExecutor",
        "org.hswebframework.web.i18n",
        "org.jetlinks.core.trace",
        "io.netty.util.concurrent.AbstractEventExecutor",
        "io.netty.util.concurrent.SingleThreadEventExecutor",
        "java.util.concurrent.ScheduledThreadPoolExecutor",
        "io.netty.util.concurrent.FastThreadLocalRunnable",
        "java.lang.Thread",
    };

    public static boolean isUnimportant(StackTraceElement element) {
        for (String unimportantPackage : unimportantPackages) {
            if (element.getClassName().startsWith(unimportantPackage)) {
                return true;
            }
        }
        return false;
    }

    public static void writeStackTraceElement(StringBuilder builder,
                                              StackTraceElement[] elements) {
        int unimportantCount = 0;
        for (StackTraceElement element : elements) {
            if (compactEnabled && isUnimportant(element)) {
                unimportantCount++;
                continue;
            }
            if (unimportantCount > 0) {
                builder.append("\t...")
                       .append(unimportantCount)
                       .append(" frames excluded\n");
                unimportantCount = 0;
            }
            builder.append("\tat ")
                   .append(element)
                   .append("\n");
        }

        if (unimportantCount > 0) {
            builder.append("\t...")
                   .append(unimportantCount)
                   .append(" frames excluded\n");
        }
    }

    public static String getStackTrace(Throwable e) {
        if (e == null) {
            return "";
        }
        return getStackTrace(new StringBuilder(), e).toString();
    }

    public static StringBuilder getStackTrace(StringBuilder builder,
                                              Throwable e) {
        builder.append(e)
               .append("\n");

        StackTraceElement[] elements = e.getStackTrace();
        if (elements != null && elements.length != 0) {
            writeStackTraceElement(builder, elements);
        }

        for (Throwable throwable : e.getSuppressed()) {
            builder.append("Suppressed: ");
            getStackTrace(builder, throwable);
        }

        Throwable cause = e.getCause();
        if (cause != null) {
            builder.append("Caused by: ");
            getStackTrace(builder, cause);
        }

        return builder;
    }


}
