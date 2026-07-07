package common.configs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {

    private static final Config INSTANSE = new Config();
    private Properties properties = new Properties();

    private Config() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("config.properties not founded");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Fail to load config.properties", e);
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
