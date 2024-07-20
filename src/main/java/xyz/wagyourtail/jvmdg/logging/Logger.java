package xyz.wagyourtail.jvmdg.logging;

import xyz.wagyourtail.jvmdg.util.Consumer;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;

public class Logger {
    private final String prefix;
    private final Level level;
    private final boolean useAnsiColors;
    private final PrintStream out;

    public Logger(String prefix, Level level, boolean useAnsiColors, PrintStream out) {
        this.prefix = prefix;
        this.level = level;
        this.out = out;
        this.useAnsiColors = useAnsiColors;
    }

    public Logger(Class<?> clazz, Level level, boolean useAnsiColors, PrintStream out) {
        this(clazz.getSimpleName(), level, useAnsiColors, out);
    }

    public boolean is(Level level) {
        return level.ordinal() >= this.level.ordinal();
    }

    public void log(Level level, String message) {
        if (is(level)) {
            String messageContent = "[" + prefix + "] " + level + ": " + message;
            if (useAnsiColors) {
                out.println(level.ansiColor(messageContent));
            } else {
                out.println(messageContent);
            }
        }
    }

    public void trace(String message) {
        log(Level.TRACE, message);
    }

    public void debug(String message) {
        log(Level.DEBUG, message);
    }

    public void info(String message) {
        log(Level.INFO, message);
    }

    public void warn(String message) {
        log(Level.WARN, message);
    }

    public void error(String message) {
        log(Level.ERROR, message);
    }

    public void fatal(String message) {
        log(Level.FATAL, message);
    }

    public void warn(final String message, final Throwable t) {
        wrapPrintStream(Level.WARN, new Consumer<PrintStream>() {
            @Override
            public void accept(PrintStream printStream) {
                printStream.println(message);
                t.printStackTrace(printStream);
            }
        });
    }

    public void error(final String message, final Throwable t) {
        wrapPrintStream(Level.ERROR, new Consumer<PrintStream>() {
            @Override
            public void accept(PrintStream printStream) {
                printStream.println(message);
                t.printStackTrace(printStream);
            }
        });
    }

    public void fatal(final String message, final Throwable t) {
        wrapPrintStream(Level.FATAL, new Consumer<PrintStream>() {
            @Override
            public void accept(PrintStream printStream) {
                printStream.println(message);
                t.printStackTrace(printStream);
            }
        });
    }

    public Logger subLogger(String prefix) {
        return new Logger(this.prefix + "/" + prefix, level, useAnsiColors, out);
    }

    public Logger subLogger(Class<?> clazz) {
        return subLogger(clazz.getSimpleName());
    }

    public void wrapPrintStream(Level level, Consumer<PrintStream> ps) {
        if (is(level)) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps2 = new PrintStream(baos, true, StandardCharsets.UTF_8.name());
                ps.accept(ps2);
                ps2.close();
                String str = baos.toString(StandardCharsets.UTF_8.name());
                log(level, str);
            } catch (UnsupportedEncodingException e) {
                Utils.<RuntimeException>sneakyThrow(e);
            }
        }
    }

    public enum Level {
        TRACE(AnsiColor.DARK_GRAY),
        DEBUG(AnsiColor.LIGHT_GRAY),
        INFO(AnsiColor.WHITE),
        WARN(AnsiColor.YELLOW),
        ERROR(AnsiColor.RED),
        FATAL(AnsiColor.LIGHT_RED);
        ;

        private final AnsiColor ansiColor;

        Level(AnsiColor ansiColor) {
            this.ansiColor = ansiColor;
        }

        public AnsiColor getAnsiColor() {
            return ansiColor;
        }

        public String ansiColor(String message) {
            return ansiColor.wrap(message);
        }
    }

    public enum AnsiColor {
        RESET("\u001B[0m"),
        BLACK("\u001B[30m"),
        RED("\u001B[31m"),
        GREEN("\u001B[32m"),
        YELLOW("\u001B[33m"),
        BLUE("\u001B[34m"),
        PURPLE("\u001B[35m"),
        CYAN("\u001B[36m"),
        LIGHT_GRAY("\u001B[37m"),

        DARK_GRAY("\u001B[90m"),
        LIGHT_RED("\u001B[91m"),
        LIGHT_GREEN("\u001B[92m"),
        LIGHT_YELLOW("\u001B[93m"),
        LIGHT_BLUE("\u001B[94m"),
        LIGHT_PURPLE("\u001B[95m"),
        LIGHT_CYAN("\u001B[96m"),
        WHITE("\u001B[97m");

        private final String ansiColor;

        AnsiColor(String ansiColor) {
            this.ansiColor = ansiColor;
        }

        public String getAnsiColor() {
            return ansiColor;
        }

        public String wrap(String message) {
            String[] parts = message.split("\n");
            StringBuilder sb = new StringBuilder();
            Iterator<String> it = Arrays.asList(parts).iterator();
            while (it.hasNext()) {
                sb.append(ansiColor).append(it.next()).append(RESET.ansiColor);
                if (it.hasNext()) sb.append("\n");
            }
            return sb.toString();
        }

    }
}
