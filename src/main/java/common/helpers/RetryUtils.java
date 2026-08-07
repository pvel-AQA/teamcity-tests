package common.helpers;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.ByteArrayInputStream;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class RetryUtils {

    private RetryUtils() { }

    public static <T> T retry(
            String title,
            Supplier<T> action,
            Predicate<T> condition,
            int maxAttempts,
            long delayMillis) {

        T result = null;
        int attempts = 0;

        while (attempts < maxAttempts) {
            attempts++;
            final int currentAttempt = attempts;

            try {
                // 1. Оборачиваем И action.get(), И проверку condition внутри шага Allure
                result = StepLogger.log("Attempt " + currentAttempt + ": " + title, () -> {
                    T actResult = action.get();

                    if (!condition.test(actResult)) {
                        // Если условие не выполнено — кидаем ошибку ВНУТРИ шага Allure,
                        // чтобы этот шаг в отчете стал КРАСНЫМ
                        throw new AssertionError("Condition not met. Current state: " + actResult);
                    }
                    return actResult;
                });

                // Если шаг прошел без ошибок — значит условие выполнено, возвращаем результат
                return result;

            } catch (Throwable e) {
                // 2. Если попытка упала — снимаем скриншот (если браузер запущен)
                attachScreenshot("Screenshot - Attempt " + currentAttempt);

                // Если это была последняя попытка — пробрасываем ошибку дальше
                if (currentAttempt == maxAttempts) {
                    throw new AssertionError("Retry failed: " + title + " after " + maxAttempts + " attempts", e);
                }
            }

            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        throw new AssertionError("Retry failed: " + title + " after " + maxAttempts + " attempts");
    }

    private static void attachScreenshot(String name) {
        try {
            if (WebDriverRunner.hasWebDriverStarted()) {
                byte[] screenshot = ((TakesScreenshot) WebDriverRunner.getWebDriver())
                        .getScreenshotAs(OutputType.BYTES);

                Allure.addAttachment(
                        name,
                        "image/png",
                        new ByteArrayInputStream(screenshot),
                        ".png"
                );
            }
        } catch (Exception ignored) {
            // Игнорируем ошибки при снятии скриншота, чтобы не свалить сам retry
        }
    }

    public static <T> T retryStable(
            String title,
            Supplier<T> action,
            BiPredicate<T, T> isStable,
            int maxAttempts,
            long delayMillis
    ) {
        T previous = null;
        T current = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            current = StepLogger.log("Attempt " + attempt + ": " + title, () -> action.get());

            if (previous != null && isStable.test(previous, current)) {
                return current;
            }

            previous = current;
            Selenide.sleep(delayMillis);
        }

        throw new RuntimeException("Value did not stabilize after " + maxAttempts + " attempts");
    }
}
