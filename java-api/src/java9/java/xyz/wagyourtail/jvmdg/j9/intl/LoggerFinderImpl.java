package xyz.wagyourtail.jvmdg.j9.intl;


import xyz.wagyourtail.jvmdg.j9.stub.java_base.J_L_Module;
import xyz.wagyourtail.jvmdg.j9.stub.java_base.J_L_System;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LoggerFinderImpl extends J_L_System.LoggerFinder {
    public static final J_L_System.LoggerFinder INSTANCE = new LoggerFinderImpl();

    private static Level getLevel(J_L_System.Logger.Level level) {
        switch (level) {
            case ALL:
                return Level.ALL;
            case TRACE:
                return Level.FINER;
            case DEBUG:
                return Level.FINE;
            case INFO:
                return Level.INFO;
            case WARNING:
                return Level.WARNING;
            case ERROR:
                return Level.SEVERE;
            case OFF:
                return Level.OFF;
        }
        throw new IllegalArgumentException("Unknown level: " + level);
    }

    @Override
    public J_L_System.Logger getLogger(String name, J_L_Module module) {
        return new LoggerImpl(name, module);
    }

    public static class LoggerImpl implements J_L_System.Logger {
        public final Logger logger;
        public final J_L_Module module;

        public LoggerImpl(String name, J_L_Module module) {
            logger = Logger.getLogger(name);
            this.module = module;
        }

        private static Pair<Class<?>, String> getCallerWithMethodName(MethodHandles.Lookup lookup) throws ClassNotFoundException {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (int i = 2; i < stack.length; i++) {
                String className = stack[i].getClassName();
                if (!Utils.isReflectionFrame(className)) {
                    Class<?> cls = Class.forName(className);
                    if (!J_L_System.Logger.class.isAssignableFrom(cls)) {
                        return new Pair<>(cls, stack[i].getMethodName());
                    }
                }
            }
            throw new ClassNotFoundException("Could not find caller class???");
        }

        @Override
        public String getName() {
            return logger.getName();
        }

        @Override
        public boolean isLoggable(Level level) {
            return logger.isLoggable(getLevel(level));
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String message, Throwable thrown) {
            LogRecord record = new LogRecord(getLevel(level), message);
            record.setThrown(thrown);
            try {
                Pair<Class<?>, String> caller = getCallerWithMethodName(MethodHandles.lookup());
                record.setSourceClassName(caller.getFirst().getName());
                record.setSourceMethodName(caller.getSecond());
            } catch (Throwable e) {
                Utils.sneakyThrow(e);
            }
            record.setResourceBundle(bundle);
            logger.log(record);
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String format, Object... args) {
            LogRecord record = new LogRecord(getLevel(level), MessageFormat.format(format, args));
            try {
                Pair<Class<?>, String> caller = getCallerWithMethodName(MethodHandles.lookup());
                record.setSourceClassName(caller.getFirst().getName());
                record.setSourceMethodName(caller.getSecond());
            } catch (Throwable e) {
                Utils.sneakyThrow(e);
            }
            record.setResourceBundle(bundle);
            logger.log(record);
        }

    }

}
