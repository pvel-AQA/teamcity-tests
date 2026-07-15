package common.configs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {

    private static final Config INSTANSE = new Config();
    private Properties properties = new Properties();

    public static final String ADMIN_USERNAME = Config.getProperty("admin.username");
    public static final String ADMIN_PASSWORD = Config.getProperty("admin.password");
    //public static final String ADMIN_TOKEN = Config.getProperty("admin.token");
    public static final String ADMIN_TOKEN = Config.getProperty("teamcity.auth.token");
    public static final String API_PREFIX = Config.getProperty("api.prefix");
    public static final String TEAMCITY_SERVER_NAME = getProperty("teamcity.server");

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

        return INSTANSE.properties.getProperty(key);
    }
}
