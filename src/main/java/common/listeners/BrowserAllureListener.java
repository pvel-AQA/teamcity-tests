package common.listeners;

import common.configs.Config;
import io.qameta.allure.listener.TestLifecycleListener;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.TestResult;

public class BrowserAllureListener implements TestLifecycleListener {
    @Override
    public void beforeTestSchedule(TestResult result) {
        String browser = System.getProperty("browser");
        if (browser == null || browser.isBlank()) {
            browser = Config.getProperty("browser");
        }
        // 1. Меняем имя, чтобы визуально тесты отличались в списке
        result.setName(result.getName() + " [" + browser + "]");
        // 2. Строим правильный historyId, чтобы тесты не склеивались
        result.setHistoryId(result.getHistoryId() + "-" + browser);
        // 3. Добавляем Label для вашего allurerc.js
        result.getLabels().add(new Label().setName("browser").setValue(browser));
        // 4. Добавляем как параметр (опционально, для красоты в UI)
        result.getParameters().add(new io.qameta.allure.model.Parameter()
                .setName("browser")
                .setValue(browser));
    }
}
