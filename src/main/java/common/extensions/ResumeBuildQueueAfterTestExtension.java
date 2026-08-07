package common.extensions;

import api.steps.UserSteps;
import common.annotations.ResumeBuildQueueAfterTest;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ResumeBuildQueueAfterTestExtension implements AfterEachCallback {
    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        ResumeBuildQueueAfterTest annotation = context.getRequiredTestMethod().getAnnotation(ResumeBuildQueueAfterTest.class);
        if (annotation != null) {
            UserSteps.resumeBuildQueue();
        }
    }
}
