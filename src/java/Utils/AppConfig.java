package Utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Single entry point for secrets and environment-dependent settings.
 *
 * Lookup order for every key: environment variable, then JVM system property, then the properties file
 * {@code %USERPROFILE%\.aistudyhub\app.properties} (override the path with the env var / system property
 * {@code AISTUDYHUB_CONFIG}). The file lives outside the repo; see docs/remediation/app.properties.example.
 *
 * A blank value counts as "not set".
 */
public final class AppConfig {

    private static final Logger LOG = Logger.getLogger(AppConfig.class.getName());

    public static final String CONFIG_PATH_KEY = "AISTUDYHUB_CONFIG";

    private static final Object LOCK = new Object();
    private static volatile Properties fileProps;

    private AppConfig() {
    }

    /** Returns the value, or null when the key is not set anywhere. */
    public static String get(String key) {
        String value = System.getenv(key);
        if (isBlank(value)) {
            value = System.getProperty(key);
        }
        if (isBlank(value)) {
            value = loadedFile().getProperty(key);
        }
        return isBlank(value) ? null : value.trim();
    }

    /** For non-secret values that have a documented safe default. */
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    /** Fails closed: throws when the key is missing, and the message names the key (never a value). */
    public static String require(String key) {
        String value = get(key);
        if (value == null) {
            throw new IllegalStateException("Missing required config: " + key);
        }
        return value;
    }

    /** Accepts only "true" or "false" (case-insensitive); anything else throws. */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalStateException("Config " + key + " must be true or false");
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Config " + key + " must be an integer");
        }
    }

    public static boolean has(String key) {
        return get(key) != null;
    }

    /** Required keys that are currently missing. Used by the startup validator. */
    public static List<String> missing(String... keys) {
        ArrayList<String> out = new ArrayList<>();
        for (String key : keys) {
            if (!has(key)) {
                out.add(key);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /** Names of the given keys that are set (for logging; never values). */
    public static List<String> present(String... keys) {
        ArrayList<String> out = new ArrayList<>();
        for (String key : keys) {
            if (has(key)) {
                out.add(key);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /** Forget the cached properties file so the next lookup re-reads it. */
    public static void reload() {
        synchronized (LOCK) {
            fileProps = null;
        }
    }

    /** Where the properties file is expected, whether or not it exists. */
    public static Path configFilePath() {
        String override = System.getenv(CONFIG_PATH_KEY);
        if (isBlank(override)) {
            override = System.getProperty(CONFIG_PATH_KEY);
        }
        if (!isBlank(override)) {
            return Paths.get(override.trim());
        }
        return Paths.get(System.getProperty("user.home"), ".aistudyhub", "app.properties");
    }

    private static Properties loadedFile() {
        Properties props = fileProps;
        if (props == null) {
            synchronized (LOCK) {
                props = fileProps;
                if (props == null) {
                    props = readFile();
                    fileProps = props;
                }
            }
        }
        return props;
    }

    private static Properties readFile() {
        Properties props = new Properties();
        Path path = configFilePath();
        if (!Files.isRegularFile(path)) {
            LOG.log(Level.INFO, "No config file at {0}; using environment variables and system properties only", path);
            return props;
        }
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
            LOG.log(Level.INFO, "Loaded config file {0} ({1} keys)", new Object[]{path, props.size()});
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Could not read config file " + path, e);
        }
        return props;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
