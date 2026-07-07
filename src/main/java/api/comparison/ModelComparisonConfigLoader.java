package api.comparison;

import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ModelComparisonConfigLoader {

    private static final Map<String, ComparisonRule> rules = new HashMap<>();

    public ModelComparisonConfigLoader(String configFile) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(configFile)) {
            if (input == null) {
                throw new RuntimeException("comparison.properties not founded");
            }
            Properties properties = new Properties();
            properties.load(input);
            for (String key : properties.stringPropertyNames()) {
                String[] target = properties.getProperty(key).split(":");
                if (target.length != 2) {
                    continue;
                }
                var responseNameClass = target[0].trim();
                List<String> fieldsPairs = Arrays.asList(target[1].split(","));

                rules.put(key.trim(), new ComparisonRule(responseNameClass, fieldsPairs));
            }
        } catch (IOException e) {
            throw new RuntimeException("Fail to load config.properties", e);
        }
    }

    public ComparisonRule getRuleFor(Class<?> requestClass) {
        return rules.get(requestClass.getSimpleName());
    }


    public static class ComparisonRule {
        private final String responseClassName;
        @Getter
        private final Map<String, String> fieldMappings;

        public ComparisonRule(String responseClassName, List<String> fieldPairs) {
            this.responseClassName = responseClassName;
            this.fieldMappings = new HashMap<>();

            for (String pair : fieldPairs) {
                String[] parts = pair.split("=");
                if (parts.length == 2) {
                    fieldMappings.put(parts[0].trim(), parts[1].trim());
                } else {
                    fieldMappings.put(pair.trim(), pair.trim());
                }
            }
        }

    }

}
