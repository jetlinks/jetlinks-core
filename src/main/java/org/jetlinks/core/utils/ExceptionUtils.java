package org.jetlinks.core.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ExceptionUtils {

    public static final boolean compactEnabled =
        Boolean.parseBoolean(
            System.getProperty("jetlinks.exception.compact.enabled", "true")
        );

    public static int maxStackLineSize = 25;

    // 无关紧要的异常栈信息
    private static final List<String> unimportantPackages = new CopyOnWriteArrayList<>(
        Arrays.asList(
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
            "jdk.nashorn"
        )
    );

    public static void addUnimportantPackage(String... packageName) {
        unimportantPackages.addAll(Arrays.asList(packageName));
    }

    public static boolean isUnimportant(String className) {
        for (String unimportantPackage : unimportantPackages) {
            if (className.startsWith(unimportantPackage)) {
                return true;
            }
        }
        return false;
    }

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
        int total = 0;
        for (StackTraceElement element : elements) {
            if (compactEnabled && total++ > 2 && isUnimportant(element)) {
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
        return getStackTrace(new StringBuilder(1024), e).toString();
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


    private static void fillStackTrace(Throwable e, List<StackTraceElement> container) {
        for (StackTraceElement traceElement : e.getStackTrace()) {
            if (ExceptionUtils.isUnimportant(traceElement)) {
                continue;
            }
            container.add(traceElement);
        }
    }

    public static StackTraceElement[] getMergedStackTrace(Throwable e) {
        List<StackTraceElement> elements = new ArrayList<>(64);
        getFullStackTrace(e, elements);
        return elements.toArray(new StackTraceElement[0]);
    }


    public static String throwableToString(Throwable e) {
        return omitMessage(e.toString());
    }

    public static String omitMessage(String str) {
        if (str == null || str.length() <= 4096) {
            return str;
        }
        return headTail(str, maxStackLineSize);
    }

    private static String headTail(String text, int n) {
        if (text == null || text.isEmpty()) return text;

        int len = text.length();

        // ---------- 1. 从头找前 N 行 ----------
        int headEnd = -1;
        int lineCount = 0;

        for (int i = 0; i < len; i++) {
            if (text.charAt(i) == '\n') {
                lineCount++;
                if (lineCount == n) {
                    headEnd = i + 1; // 包含换行
                    break;
                }
            }
        }

        // 如果总行数 <= 2N，直接返回
        if (headEnd < 0) {
            return text;
        }

        // ---------- 2. 从尾找后 N 行 ----------
        int tailStart = -1;
        lineCount = 0;

        for (int i = len - 1; i >= 0; i--) {
            if (text.charAt(i) == '\n') {
                lineCount++;
                if (lineCount == n) {
                    tailStart = i + 1;
                    break;
                }
            }
        }

        if (tailStart < 0 || tailStart <= headEnd) {
            return text;
        }

        // ---------- 3. 统计省略的行数 ----------
        int omitted = 0;
        for (int i = headEnd; i < tailStart; i++) {
            if (text.charAt(i) == '\n') {
                omitted++;
            }
        }

        // ---------- 4. 拼接最终结果 ----------

        return text.substring(0, headEnd) +
            "... omitted " + omitted + " lines ...\n" +
            text.substring(tailStart, len);
    }

    private static void getFullStackTrace(Throwable e, List<StackTraceElement> elements) {

        fillStackTrace(e, elements);

        for (Throwable throwable : e.getSuppressed()) {
            elements.add(
                new StackTraceElement(
                    "Suppressed: " + throwableToString(throwable),
                    "",
                    null,
                    -1
                )
            );
            fillStackTrace(throwable, elements);
        }

        Throwable cause = e.getCause();
        if (cause != null) {
            elements.add(
                new StackTraceElement(
                    "Caused by: " + throwableToString(cause),
                    "",
                    null,
                    -1
                )
            );
            getFullStackTrace(cause, elements);
        }

    }


}
