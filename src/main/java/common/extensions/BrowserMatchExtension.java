package common.extensions;

import com.codeborne.selenide.Configuration;
import common.annotations.Browsers;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Arrays;


public class BrowserMatchExtension implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Browsers annotation = context.getElement().map(el -> el.getAnnotation(Browsers.class))
                .orElseThrow(null);

        if (annotation == null) {
            return ConditionEvaluationResult.enabled("There are no browser restrictions.");
        }

        String currentBrowser = Configuration.browser;

        boolean matches = Arrays.stream(annotation.value())
                .anyMatch(browser -> browser.equals(currentBrowser));

        if (matches) {
            return ConditionEvaluationResult.enabled("The current browser satisfies the condition: " + currentBrowser);
        } else {
            return ConditionEvaluationResult.disabled("The test was skipped because the current browser does not meet the condition." + currentBrowser
                    + " is missing from the list of browsers allowed for the test:  " + Arrays.toString(annotation.value()));
        }

    }
}
