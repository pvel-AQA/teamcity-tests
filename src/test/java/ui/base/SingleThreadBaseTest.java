package ui.base;

import api.models.agent.GetAgentsResponse;
import api.models.build.BuildQueueResponse;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.ValidatedCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import common.helpers.RetryUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Map;

@Execution(ExecutionMode.SAME_THREAD)
public class SingleThreadBaseTest extends BaseUiTest {

    @BeforeEach
    public void waitForQueueAndAgentReady() {
        RetryUtils.retry(
                "Wait until TeamCity agent is connected and ready",
                () -> {
                    GetAgentsResponse response = new ValidatedCrudRequester<GetAgentsResponse>(
                            RequestSpec.withAuthExtensionUser(),
                            Endpoint.AGENTS,
                            ResponseSpec.returnsOk()
                    ).get(Map.of("fields", "agent(id,name,connected,authorized,enabled)"));

                    if (response.getAgent() == null || response.getAgent().isEmpty()) {
                        return false;
                    }

                    var agent = response.getAgent().get(0);
                    return agent.isConnected() && agent.isAuthorized() && agent.isEnabled();
                },
                isReady -> isReady,
                30,
                1000
        );

        RetryUtils.retry(
                "Wait until build queue is empty",
                () -> {
                    BuildQueueResponse queue = new ValidatedCrudRequester<BuildQueueResponse>(
                            RequestSpec.withAuthExtensionUser(),
                            Endpoint.BUILD_QUEUE_GET,
                            ResponseSpec.returnsOk()
                    ).get();

                    return queue.getCount() == 0;
                },
                isEmpty -> isEmpty,
                30,
                1000
        );
    }
}
