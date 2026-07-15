package api.agent;

import api.models.agent.GetAgentsResponse;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.request.skelethon.requester.ValidatedCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import base.BaseTest;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import org.junit.jupiter.api.Test;

public class AgentTest extends BaseTest {
    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void AgentCanBeConnectedToTheServerTest() {
        var getAgentsResponse = new ValidatedCrudRequester<GetAgentsResponse>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.AGENTS,
                ResponseSpec.returnsOk()
        ).get(new CrudRequester.QueryBuilder().locatorEqualsAuthorizedAny().build());

        softy.assertThat(getAgentsResponse.getCount()).isOne();
    }
}
