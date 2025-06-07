package warcaby.utils;

public class ApplicationConfig {
    private static final Logger logger = new Logger(ApplicationConfig.class);

    public static void configureLogging(boolean enabled, Logger.Level level) {
        Logger.setLoggingEnabled(enabled);
        if (enabled) {
            Logger.setMinimumLevel(level);
            logger.info("Logowanie skonfigurowane. Włączone: " + enabled + ", Poziom: " + level.getName());
        } else {
            // Ten log się nie pojawi, jeśli logowanie jest wyłączone
        }
    }

    public static void initializeDefaults() {
        configureLogging(true, Logger.Level.INFO);
        logger.info("Aplikacja skonfigurowana z domyślnymi ustawieniami.");
    }

    public static void enableFullLogging() {
        configureLogging(true, Logger.Level.DEBUG);
    }

    public static void disableLogging() {
        configureLogging(false, Logger.Level.NONE);
    }
}