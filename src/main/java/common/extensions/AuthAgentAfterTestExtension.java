package common.extensions;

import api.steps.UserSteps;
import common.annotations.AuthAgentAfterTest;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class AuthAgentAfterTestExtension implements AfterEachCallback {
    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        AuthAgentAfterTest annotation = context.getRequiredTestMethod().getAnnotation(AuthAgentAfterTest.class);
        if (annotation != null) {
            int agentId = UserSteps.getAgentId();
            UserSteps.authorizeAgent(agentId);
        }
    }
}
