package org.jetlinks.core.monitor.logger;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.Marker;

@AllArgsConstructor
class BridgeLoggerSlf4j implements Logger {
    private final org.jetlinks.core.monitor.logger.Logger logger;

    @Override
    public String getName() {
        return logger.toString();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String s) {
        logger.trace(s);
    }

    @Override
    public void trace(String s, Object o) {
        logger.trace(s, o);
    }

    @Override
    public void trace(String s, Object o, Object o1) {
        logger.trace(s, o, o1);
    }

    @Override
    public void trace(String s, Object... objects) {
        logger.trace(s, objects);
    }

    @Override
    public void trace(String s, Throwable throwable) {
        logger.trace(s, throwable);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(Marker marker, String s) {
        logger.trace(s);
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        logger.trace(s, o);
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o1) {
        logger.trace(s, o, o1);
    }

    @Override
    public void trace(Marker marker, String s, Object... objects) {
        logger.trace(s, objects);
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        logger.trace(s, throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        logger.debug(s);
    }

    @Override
    public void debug(String s, Object o) {
        logger.debug(s, o);
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        logger.debug(s, o, o1);
    }

    @Override
    public void debug(String s, Object... objects) {
        logger.debug(s, objects);
    }

    @Override
    public void debug(String s, Throwable throwable) {
        logger.debug(s, throwable);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return isDebugEnabled();
    }

    @Override
    public void debug(Marker marker, String s) {
        logger.debug(s);
    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        logger.debug(s, o);
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o1) {
        logger.debug(s, o, o1);
    }

    @Override
    public void debug(Marker marker, String s, Object... objects) {
        logger.debug(s, objects);
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        logger.debug(s, throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String s) {
        logger.info(s);
    }

    @Override
    public void info(String s, Object o) {
        logger.info(s, o);
    }

    @Override
    public void info(String s, Object o, Object o1) {
        logger.info(s,o,o1);
    }

    @Override
    public void info(String s, Object... objects) {
        logger.info(s, objects);
    }

    @Override
    public void info(String s, Throwable throwable) {
        logger.info(s, throwable);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(Marker marker, String s) {
        logger.info(s);
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        logger.info(s,o);
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o1) {
        logger.info(s,o,o1);
    }

    @Override
    public void info(Marker marker, String s, Object... objects) {
        logger.info(s, objects);
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        logger.info(s, throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String s) {
        logger.warn(s);
    }


    @Override
    public void warn(String s, Object o) {
        logger.warn(s, o);
    }

    @Override
    public void warn(String s, Object... objects) {
        logger.warn(s, objects);
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        logger.warn(s, o, o1);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        logger.warn(s, throwable);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(Marker marker, String s) {
        logger.warn(s);
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        logger.warn(s, o);
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o1) {
        logger.warn(s, o, o1);
    }

    @Override
    public void warn(Marker marker, String s, Object... objects) {
        logger.warn(s, objects);
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        logger.warn(s, throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String s) {
        logger.error(s);
    }

    @Override
    public void error(String s, Object o) {
        logger.error(s, o);
    }

    @Override
    public void error(String s, Object o, Object o1) {
        logger.error(s, o, o1);
    }

    @Override
    public void error(String s, Object... objects) {
        logger.error(s, objects);
    }

    @Override
    public void error(String s, Throwable throwable) {
        logger.error(s, throwable);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(Marker marker, String s) {
        logger.error(s);
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        logger.error(s, o);
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o1) {
        logger.error(s, o, o1);
    }

    @Override
    public void error(Marker marker, String s, Object... objects) {
        logger.error(s, objects);
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        logger.error(s, throwable);
    }
}
