package common.utils;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class RetryUtils {

    public static <T> T retry(
            Supplier<T> action,
            Predicate<T> condition,
            int maxAttempts,
            long delayMillis) {
        T result = null;

        int attemps = 0;
        while (attemps < maxAttempts) {
            attemps++;
            result = action.get();
            if (condition.test(result)) {
                return result;
            }
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Retry failed after " + maxAttempts + " attempts");
    }
}
