package common.helpers;

import io.qameta.allure.Allure;
import io.restassured.response.ValidatableResponse;

import java.util.function.Supplier;

public class StepLogger {

    @FunctionalInterface
    public interface ThrowableRunnable<T> {
        T run() throws Throwable;
    }

    @FunctionalInterface
    public interface ThrowableVoidRunnable {
        void run() throws Throwable;
    }

    public static <T> T log(String title, ThrowableRunnable<T> runnable) {
        return Allure.step(title, () -> runnable.run());
    }

    public static void log(String title, ThrowableVoidRunnable runnable) {
        Allure.step(title, () -> {
            runnable.run();
            return null;
        });
    }
}
