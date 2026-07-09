package common.configs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Config {
    public static final String ADMIN_USERNAME = "admin.username";
    public static final String ADMIN_PASSWORD = "admin.password";
    public static final String ADMIN_TOKEN = "admin.token";

    private static final Config INSTANCE = new Config();
    private final Properties properties = new Properties();

    private Config() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("config.properties not found in resources");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    public static String getProperty(String key) {
        String systemValue = System.getProperty(key);
        if (systemValue != null) {
            return systemValue;
        }

        String envKey = key.replaceAll("\\.", "_").toUpperCase();
        String envValue = System.getenv(envKey);
        if (envValue != null) {
            return envValue;
        }

        return INSTANCE.properties.getProperty(key);
    }
}
