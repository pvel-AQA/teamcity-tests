package api.comparison;

import java.lang.reflect.Field;
import java.util.*;

public class ModelComparator {

    public static Object getFieldValue(Object obj, String fieldName) {
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot access field: " + fieldName, e);
            }
        }
        throw new RuntimeException("Field not found: " + fieldName + " in class " + obj.getClass().getName());
    }

    public static <A, B> ComparisonResult compareFields(A request, B response, Map<String, String> fieldMappings) {
        List<Mismatch> mismatches = new ArrayList<>();

        for (Map.Entry<String, String> entry : fieldMappings.entrySet()) {
            String requestField = entry.getKey();
            String responseField = entry.getValue();

            Object value1 = getFieldValue(request, requestField);
            Object value2 = getFieldValue(response, responseField);

            boolean isMatch;

            // ИСПРАВЛЕНИЕ: Специальная обработка для точного сравнения денег
            if (value1 instanceof java.math.BigDecimal && value2 instanceof java.math.BigDecimal) {
                isMatch = ((java.math.BigDecimal) value1).compareTo((java.math.BigDecimal) value2) == 0;
            } else {
                // Для всех остальных типов оставляем старую логику через String.valueOf
                isMatch = Objects.equals(String.valueOf(value1), String.valueOf(value2));
            }

            if (!isMatch) {
                mismatches.add(new Mismatch(requestField + " -> " + responseField, value1, value2));
            }
        }

        return new ComparisonResult(mismatches);
    }

    public static class ComparisonResult {
        private final List<Mismatch> mismatches;

        public ComparisonResult(List<Mismatch> mismatches) {
            this.mismatches = mismatches;
        }

        public boolean isSuccess() {
            return mismatches.isEmpty();
        }

        public List<Mismatch> getMismatches() {
            return mismatches;
        }

        @Override
        public String toString() {
            if (isSuccess()) {
                return "All fields match.";
            }
            StringBuilder sb = new StringBuilder("Mismatched fields:\n");
            for (Mismatch m : mismatches) {
                sb.append("- ").append(m.fieldName)
                        .append(": expected=").append(m.expected)
                        .append(", actual=").append(m.actual).append("\n");
            }
            return sb.toString();
        }
    }

    public static class Mismatch {
        public final String fieldName;
        public final Object expected;
        public final Object actual;

        public Mismatch(String fieldName, Object expected, Object actual) {
            this.fieldName = fieldName;
            this.expected = expected;
            this.actual = actual;
        }
    }

}
