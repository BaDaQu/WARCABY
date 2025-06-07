package warcaby.utils;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    public enum Level {
        DEBUG(1, "DEBUG"),
        INFO(2, "INFO "),
        WARNING(3, "WARN "),
        ERROR(4, "ERROR"),
        NONE(5, "NONE ");

        private final int value;
        private final String name;

        Level(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public String getName() {
            return name.trim(); // Zwracaj bez spacji
        }
    }

    private static boolean loggingEnabled = true;
    private static Level minimumLevel = Level.INFO;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final String className;

    @FunctionalInterface
    private interface PrintOperation {
        void print(String message);
    }

    public Logger(Class<?> clazz) {
        this.className = clazz.getSimpleName();
    }

    public static void setLoggingEnabled(boolean enabled) {
        loggingEnabled = enabled;
    }

    public static boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public static void setMinimumLevel(Level level) {
        minimumLevel = level;
    }

    public static Level getMinimumLevel() {
        return minimumLevel;
    }

    private void log(Level level, String message) {
        if (loggingEnabled && level.getValue() >= minimumLevel.getValue()) {
            String timestamp = LocalDateTime.now().format(formatter);
            String formattedMessage = String.format("[%s] [%s] [%s] :: %s",
                    timestamp,
                    level.getName(), // Użyj getName() bez trim() tutaj dla logu
                    className,
                    message);

            PrintOperation printOperation;
            if (level == Level.ERROR) {
                printOperation = System.err::println;
            } else {
                printOperation = System.out::println;
            }
            printOperation.print(formattedMessage);
        }
    }

    public void debug(String message) {
        log(Level.DEBUG, message);
    }

    public void info(String message) {
        log(Level.INFO, message);
    }

    public void warning(String message) {
        log(Level.WARNING, message);
    }

    public void error(String message) {
        log(Level.ERROR, message);
    }

    public void error(String message, Throwable throwable) {
        log(Level.ERROR, message + " | Wyjątek: " + throwable.getClass().getSimpleName() + " - " + throwable.getMessage());
        if (loggingEnabled && minimumLevel.getValue() <= Level.DEBUG.getValue()) {
            PrintStream errorStream = System.err;
            throwable.printStackTrace(errorStream);
        }
    }
}