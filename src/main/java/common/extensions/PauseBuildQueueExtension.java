package common.extensions;

import api.steps.UserSteps;
import common.annotations.PauseBuildQueue;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class PauseBuildQueueExtension implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        PauseBuildQueue annotation = context.getRequiredTestMethod().getAnnotation(PauseBuildQueue.class);
        if (annotation!=null) {
            UserSteps.pauseBuildQueue();
        }
    }
}
