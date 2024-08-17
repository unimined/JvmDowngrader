package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.j9.intl.LoggerFinderImpl;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Supplier;

public class J_L_System {

    @Stub(ref = @Ref("Ljava/lang/System;"))
    public static Logger getLogger(String name) throws ClassNotFoundException {
        Class<?> caller = Utils.getCaller(MethodHandles.lookup());
        return LoggerFinder.getLoggerFinder().getLogger(name, J_L_Class.getModule(caller));
    }

    @Stub(ref = @Ref("Ljava/lang/System;"))
    public static Logger getLogger(String name, ResourceBundle bundle) throws ClassNotFoundException {
        Objects.requireNonNull(bundle);
        Class<?> caller = Utils.getCaller(MethodHandles.lookup());
        return LoggerFinder.getLoggerFinder().getLocalizedLogger(name, bundle, J_L_Class.getModule(caller));
    }

    @Adapter("java/lang/System$Logger")
    public interface Logger {

        String getName();

        boolean isLoggable(Level level);

        default void log(Level level, String message) {
            log(level, null, message, (Object[]) null);
        }

        default void log(Level level, Supplier<String> message) {
            Objects.requireNonNull(message);
            if (isLoggable(level)) {
                log(level, message.get());
            }
        }

        default void log(Level level, Object object) {
            Objects.requireNonNull(object);
            if (isLoggable(level)) {
                log(level, object.toString());
            }
        }

        default void log(Level level, String message, Throwable throwable) {
            log(level, null, message, throwable);
        }

        default void log(Level level, Supplier<String> message, Throwable throwable) {
            Objects.requireNonNull(message);
            if (isLoggable(level)) {
                log(level, message.get(), throwable);
            }
        }

        default void log(Level level, String format, Object... args) {
            log(level, null, format, args);
        }

        void log(Level level, ResourceBundle bundle, String message, Throwable thrown);

        void log(Level level, ResourceBundle bundle, String format, Object... args);


        @Adapter("java/lang/System$Logger$Level")
        enum Level {
            ALL(Integer.MIN_VALUE),
            TRACE(400),
            DEBUG(500),
            INFO(800),
            WARNING(900),
            ERROR(1000),
            OFF(Integer.MAX_VALUE);

            final int level;

            Level(int level) {
                this.level = level;
            }

            public final String getName() {
                return name();
            }

            public final int getSeverity() {
                return level;
            }

        }

    }

    @Adapter("java/lang/System$LoggerFinder")
    public static abstract class LoggerFinder {

        protected LoggerFinder() {}

        public abstract Logger getLogger(String name, J_L_Module module);

        public Logger getLocalizedLogger(String name, ResourceBundle bundle, J_L_Module module) {
            Objects.requireNonNull(bundle);
            Objects.requireNonNull(module);
            Logger logger = getLogger(name, module);
            return new Logger() {
                @Override
                public String getName() {
                    return logger.getName();
                }

                @Override
                public boolean isLoggable(Level level) {
                    return logger.isLoggable(level);
                }

                @Override
                public void log(Level level, ResourceBundle unused, String message, Throwable thrown) {
                    logger.log(level, bundle, message, thrown);
                }

                @Override
                public void log(Level level, ResourceBundle unused, String format, Object... args) {
                    logger.log(level, bundle, format, args);
                }
            };
        }

        public static LoggerFinder getLoggerFinder() {
            return LoggerFinderImpl.INSTANCE;
        }



    }


}
