package helpers;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class RecursiveComparator {

    private final Set<String> ignoredFields = new HashSet<>();
    private final Map<String, String> fieldMappings = new HashMap<>(); // sourceField -> targetField
    private Object rootActual = null; // Сохраняем корневой объект

    private RecursiveComparator() {}

    public static RecursiveComparator create() {
        return new RecursiveComparator();
    }

    public RecursiveComparator ignoreFields(String... fields) {
        ignoredFields.addAll(Arrays.asList(fields));
        return this;
    }

    public RecursiveComparator withFieldMapping(String sourceField, String targetField) {
        fieldMappings.put(sourceField, targetField);
        return this;
    }

    public RecursiveComparator withFieldMappings(Map<String, String> mappings) {
        fieldMappings.putAll(mappings);
        return this;
    }

    public void isEqual(Object expected, Object actual) {
        if (expected == actual) return;
        if (expected == null || actual == null) {
            throw new AssertionError("One of objects is null");
        }

        this.rootActual = actual; // Сохраняем корневой объект
        compareObjects(expected, actual, "");
        this.rootActual = null; // Очищаем после сравнения
    }

    private void compareObjects(Object expected, Object actual, String path) {
        // Check if both are null
        if (expected == null && actual == null) return;
        if (expected == null || actual == null) {
            throw new AssertionError(formatMessage(path, "One is null, other is not"));
        }

        // Handle collections
        if (expected instanceof Collection<?> && actual instanceof Collection<?>) {
            compareCollections((Collection<?>) expected, (Collection<?>) actual, path);
            return;
        }

        // Handle maps
        if (expected instanceof Map<?, ?> && actual instanceof Map<?, ?>) {
            compareMaps((Map<?, ?>) expected, (Map<?, ?>) actual, path);
            return;
        }

        // Handle arrays
        if (expected.getClass().isArray() && actual.getClass().isArray()) {
            compareArrays(expected, actual, path);
            return;
        }

        // Handle primitive types and wrappers
        if (isPrimitiveOrWrapper(expected.getClass()) && isPrimitiveOrWrapper(actual.getClass())) {
            if (!Objects.equals(expected, actual)) {
                throw new AssertionError(formatMessage(path,
                        "Expected: " + expected + ", but was: " + actual));
            }
            return;
        }

        // Handle strings
        if (expected instanceof String && actual instanceof String) {
            if (!expected.equals(actual)) {
                throw new AssertionError(formatMessage(path,
                        "Expected: '" + expected + "', but was: '" + actual + "'"));
            }
            return;
        }

        // Handle enums
        if (expected.getClass().isEnum() && actual.getClass().isEnum()) {
            if (expected != actual) {
                throw new AssertionError(formatMessage(path,
                        "Expected: " + expected + ", but was: " + actual));
            }
            return;
        }

        // Handle custom objects
        compareCustomObjects(expected, actual, path);
    }

    private void compareCollections(Collection<?> expected, Collection<?> actual, String path) {
        if (expected.size() != actual.size()) {
            // Создаем детальное сообщение о различиях в коллекциях
            String detailedMessage = buildCollectionDifferenceMessage(expected, actual, path);
            throw new AssertionError(formatMessage(path,
                    "Collections have different sizes. Expected: " + expected.size() +
                            ", but was: " + actual.size() + "\n" + detailedMessage));
        }

        // Если размеры совпадают, сравниваем элементы по порядку
        Iterator<?> expectedIterator = expected.iterator();
        Iterator<?> actualIterator = actual.iterator();
        int index = 0;
        List<String> differences = new ArrayList<>();

        while (expectedIterator.hasNext() && actualIterator.hasNext()) {
            Object expectedItem = expectedIterator.next();
            Object actualItem = actualIterator.next();

            try {
                String itemPath = path.isEmpty() ? "[" + index + "]" : path + "[" + index + "]";
                compareObjects(expectedItem, actualItem, itemPath);
            } catch (AssertionError e) {
                differences.add("Element at index " + index + ": " + e.getMessage());
            }
            index++;
        }

        if (!differences.isEmpty()) {
            throw new AssertionError(formatMessage(path,
                    "Collections differ at following positions:\n" +
                            String.join("\n", differences)));
        }
    }

    /**
     * Строит детальное сообщение о различиях в коллекциях
     */
    private String buildCollectionDifferenceMessage(Collection<?> expected, Collection<?> actual, String path) {
        StringBuilder message = new StringBuilder();
        message.append("  Detailed differences:\n");

        // Показываем элементы, которые есть в expected, но нет в actual
        List<Object> expectedList = new ArrayList<>(expected);
        List<Object> actualList = new ArrayList<>(actual);

        // Находим элементы, которые есть в expected, но отсутствуют в actual
        List<Object> missingInActual = new ArrayList<>(expectedList);
        missingInActual.removeAll(actualList);

        // Находим элементы, которые есть в actual, но отсутствуют в expected
        List<Object> extraInActual = new ArrayList<>(actualList);
        extraInActual.removeAll(expectedList);

        if (!missingInActual.isEmpty()) {
            message.append("    - Elements missing in actual: ").append(missingInActual).append("\n");
        }

        if (!extraInActual.isEmpty()) {
            message.append("    - Extra elements in actual: ").append(extraInActual).append("\n");
        }

        // Если элементы не совпадают по порядку, показываем разницу по индексам
        if (expectedList.size() > 0 && actualList.size() > 0) {
            int minSize = Math.min(expectedList.size(), actualList.size());
            boolean hasPositionDifferences = false;

            for (int i = 0; i < minSize; i++) {
                if (!Objects.equals(expectedList.get(i), actualList.get(i))) {
                    if (!hasPositionDifferences) {
                        message.append("    - Position differences:\n");
                        hasPositionDifferences = true;
                    }
                    message.append("      Index ").append(i).append(": Expected '")
                            .append(expectedList.get(i)).append("', but was '")
                            .append(actualList.get(i)).append("'\n");
                }
            }
        }

        return message.toString();
    }

    private void compareMaps(Map<?, ?> expected, Map<?, ?> actual, String path) {
        if (expected.size() != actual.size()) {
            // Детальное сообщение для Map
            StringBuilder details = new StringBuilder();
            details.append("  Detailed differences:\n");

            // Ключи, которые есть в expected, но отсутствуют в actual
            Set<?> expectedKeys = expected.keySet();
            Set<?> actualKeys = actual.keySet();

            Set<Object> missingKeys = new HashSet<>(expectedKeys);
            missingKeys.removeAll(actualKeys);

            Set<Object> extraKeys = new HashSet<>(actualKeys);
            extraKeys.removeAll(expectedKeys);

            if (!missingKeys.isEmpty()) {
                details.append("    - Keys missing in actual: ").append(missingKeys).append("\n");
            }

            if (!extraKeys.isEmpty()) {
                details.append("    - Extra keys in actual: ").append(extraKeys).append("\n");
            }

            // Проверяем значения для общих ключей
            Set<Object> commonKeys = new HashSet<>(expectedKeys);
            commonKeys.retainAll(actualKeys);

            boolean hasValueDifferences = false;
            for (Object key : commonKeys) {
                if (!Objects.equals(expected.get(key), actual.get(key))) {
                    if (!hasValueDifferences) {
                        details.append("    - Value differences:\n");
                        hasValueDifferences = true;
                    }
                    details.append("      Key '").append(key).append("': Expected '")
                            .append(expected.get(key)).append("', but was '")
                            .append(actual.get(key)).append("'\n");
                }
            }

            throw new AssertionError(formatMessage(path,
                    "Maps have different sizes. Expected: " + expected.size() +
                            ", but was: " + actual.size() + "\n" + details.toString()));
        }

        for (Map.Entry<?, ?> entry : expected.entrySet()) {
            Object key = entry.getKey();
            if (!actual.containsKey(key)) {
                throw new AssertionError(formatMessage(path,
                        "Key '" + key + "' not found in actual map"));
            }
            String keyPath = path.isEmpty() ? "[" + key + "]" : path + "[" + key + "]";
            compareObjects(entry.getValue(), actual.get(key), keyPath);
        }
    }

    private void compareArrays(Object expected, Object actual, String path) {
        int expectedLength = Array.getLength(expected);
        int actualLength = Array.getLength(actual);

        if (expectedLength != actualLength) {
            StringBuilder details = new StringBuilder();
            details.append("  Detailed differences:\n");

            int minLength = Math.min(expectedLength, actualLength);
            boolean hasDifferences = false;

            for (int i = 0; i < minLength; i++) {
                Object expectedItem = Array.get(expected, i);
                Object actualItem = Array.get(actual, i);
                if (!Objects.equals(expectedItem, actualItem)) {
                    if (!hasDifferences) {
                        details.append("    - Position differences:\n");
                        hasDifferences = true;
                    }
                    details.append("      Index ").append(i).append(": Expected '")
                            .append(expectedItem).append("', but was '")
                            .append(actualItem).append("'\n");
                }
            }

            if (expectedLength > actualLength) {
                details.append("    - Expected has ").append(expectedLength - actualLength)
                        .append(" extra element(s) at the end\n");
            } else if (actualLength > expectedLength) {
                details.append("    - Actual has ").append(actualLength - expectedLength)
                        .append(" extra element(s) at the end\n");
            }

            throw new AssertionError(formatMessage(path,
                    "Arrays have different sizes. Expected: " + expectedLength +
                            ", but was: " + actualLength + "\n" + details.toString()));
        }

        for (int i = 0; i < expectedLength; i++) {
            Object expectedItem = Array.get(expected, i);
            Object actualItem = Array.get(actual, i);
            String itemPath = path.isEmpty() ? "[" + i + "]" : path + "[" + i + "]";
            compareObjects(expectedItem, actualItem, itemPath);
        }
    }

    private void compareCustomObjects(Object expected, Object actual, String path) {
        Class<?> expectedClass = expected.getClass();
        Class<?> actualClass = actual.getClass();

        // Get all fields including inherited
        List<Field> expectedFields = getAllFields(expectedClass);
        List<Field> actualFields = getAllFields(actualClass);

        // Create field maps
        Map<String, Field> expectedFieldMap = expectedFields.stream()
                .collect(Collectors.toMap(Field::getName, f -> f));
        Map<String, Field> actualFieldMap = actualFields.stream()
                .collect(Collectors.toMap(Field::getName, f -> f));

        // Check all expected fields
        for (Field expectedField : expectedFields) {
            String fieldName = expectedField.getName();

            // Build full path for this field
            String fullPath = path.isEmpty() ? fieldName : path + "." + fieldName;

            // Skip ignored fields
            if (isIgnored(fieldName, fullPath)) {
                continue;
            }

            try {
                expectedField.setAccessible(true);
                Object expectedValue = expectedField.get(expected);

                // Get actual value considering field mappings
                Object actualValue = getActualValueWithMapping(actual, fullPath, fieldName, actualFieldMap);

                compareObjects(expectedValue, actualValue, fullPath);

            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access field: " + fieldName, e);
            }
        }
    }

    /**
     * Получает значение из actual объекта с учетом маппинга полей
     * Поддерживает абсолютные пути (например, address.city -> address.town)
     */
    private Object getActualValueWithMapping(Object actual, String fullPath, String fieldName,
            Map<String, Field> actualFieldMap) {
        // Проверяем маппинг для полного пути
        String mappedPath = fieldMappings.get(fullPath);

        // Если нет маппинга для полного пути, проверяем для простого имени
        if (mappedPath == null) {
            mappedPath = fieldMappings.get(fieldName);
        }

        // Если есть маппинг
        if (mappedPath != null) {
            // Если маппинг содержит точки, это вложенный путь (абсолютный)
            if (mappedPath.contains(".")) {
                // Используем корневой объект для навигации по абсолютному пути
                if (rootActual == null) {
                    throw new AssertionError("Root actual object is null");
                }
                return getNestedValue(rootActual, mappedPath, fullPath);
            } else {
                // Простой маппинг - ищем поле в текущем объекте
                Field actualField = actualFieldMap.get(mappedPath);
                if (actualField == null) {
                    throw new AssertionError(formatMessage(fullPath,
                            "Field '" + mappedPath + "' not found in actual object"));
                }
                try {
                    actualField.setAccessible(true);
                    return actualField.get(actual);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to access field: " + mappedPath, e);
                }
            }
        }

        // Нет маппинга, используем имя поля как есть
        Field actualField = actualFieldMap.get(fieldName);
        if (actualField == null) {
            throw new AssertionError(formatMessage(fullPath,
                    "Field '" + fieldName + "' not found in actual object"));
        }
        try {
            actualField.setAccessible(true);
            return actualField.get(actual);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access field: " + fieldName, e);
        }
    }

    /**
     * Получает вложенное значение из объекта по абсолютному пути
     * Начинает навигацию с корневого объекта
     */
    private Object getNestedValue(Object rootObject, String path, String errorPath) {
        String[] parts = path.split("\\.");
        Object currentObject = rootObject;
        StringBuilder currentPath = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (currentPath.length() > 0) {
                currentPath.append(".");
            }
            currentPath.append(part);

            if (currentObject == null) {
                throw new AssertionError(formatMessage(errorPath,
                        "Null encountered while navigating to '" + currentPath + "'"));
            }

            try {
                Field field = findField(currentObject.getClass(), part);
                if (field == null) {
                    throw new AssertionError(formatMessage(errorPath,
                            "Field '" + part + "' not found in actual object at path '" + currentPath + "'"));
                }
                field.setAccessible(true);
                currentObject = field.get(currentObject);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access field: " + part, e);
            }
        }

        return currentObject;
    }

    /**
     * Находит поле в классе, включая родительские классы
     */
    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz == Boolean.class || clazz == Byte.class ||
                clazz == Character.class || clazz == Short.class ||
                clazz == Integer.class || clazz == Long.class ||
                clazz == Float.class || clazz == Double.class ||
                clazz == Void.class;
    }

    private boolean isIgnored(String fieldName, String fullPath) {
        // Check exact match
        if (ignoredFields.contains(fieldName) || ignoredFields.contains(fullPath)) {
            return true;
        }

        // Check wildcard pattern (e.g., "*.id" to ignore all id fields)
        for (String ignored : ignoredFields) {
            if (ignored.startsWith("*.") && fieldName.equals(ignored.substring(2))) {
                return true;
            }
            if (ignored.endsWith(".*") && fullPath.startsWith(ignored.substring(0, ignored.length() - 2))) {
                return true;
            }
        }

        return false;
    }

    private String formatMessage(String path, String message) {
        if (path.isEmpty()) {
            return message;
        }
        return "At path '" + path + "': " + message;
    }

    // Builder pattern for fluent API
    public static class RecursiveComparatorBuilder {
        private final RecursiveComparator comparator;

        private RecursiveComparatorBuilder() {
            comparator = new RecursiveComparator();
        }

        public static RecursiveComparatorBuilder builder() {
            return new RecursiveComparatorBuilder();
        }

        public RecursiveComparatorBuilder ignoreFields(String... fields) {
            comparator.ignoreFields(fields);
            return this;
        }

        public RecursiveComparatorBuilder withFieldMapping(String sourceField, String targetField) {
            comparator.withFieldMapping(sourceField, targetField);
            return this;
        }

        public RecursiveComparator build() {
            return comparator;
        }
    }

    // Example usage with AssertJ-like syntax
    public RecursiveComparatorAssert assertThat(Object actual) {
        return new RecursiveComparatorAssert(actual);
    }

    public class RecursiveComparatorAssert {
        private final Object actual;

        private RecursiveComparatorAssert(Object actual) {
            this.actual = actual;
        }

        public RecursiveComparatorAssert ignoringFields(String... fields) {
            RecursiveComparator.this.ignoreFields(fields);
            return this;
        }

        public RecursiveComparatorAssert withFieldMapping(String sourceField, String targetField) {
            RecursiveComparator.this.withFieldMapping(sourceField, targetField);
            return this;
        }

        public void isEqualTo(Object expected) {
            RecursiveComparator.this.isEqual(expected, actual);
        }
    }
}