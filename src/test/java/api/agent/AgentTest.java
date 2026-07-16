package api.agent;

import api.enums.locators.LocatorType;
import api.generators.RandomGenerator;
import api.models.agent.*;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.request.skelethon.requester.ValidatedCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import api.steps.UserSteps;
import base.BaseTest;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

@Isolated
public class AgentTest extends BaseTest {

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void agentCanBeConnectedToTheServerTest() {
        final String expectedAgentName = "teamcity-agent";

        var getAgentsResponse = new ValidatedCrudRequester<GetAgentsResponse>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.AGENTS,
                ResponseSpec.returnsOk()
        ).get(new CrudRequester.QueryBuilder()
                .locatorEqualsAuthorizedAny()
                .locatorEqualsConnectedTrue()
                .build());

        softly.assertThat(getAgentsResponse.getCount()).isEqualTo(2);
        softly.assertThat(getAgentsResponse.getAgent().getFirst().getId()).isOne();
        softly.assertThat(getAgentsResponse.getAgent().getFirst().getName()).isEqualTo(expectedAgentName);
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void agentCanBeAuthorizedTest() {
        var agentId = UserSteps.getAgentId();
        var authorizeAgentRequest = RandomGenerator.generate(AuthorizeAgentRequest.class);
        var unauthorizeAgentRequest = RandomGenerator.generate(UnauthorizeAgentRequest.class);

        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.AGENTS_AUTHORIZED_INFO,
                ResponseSpec.returnsOk()
        ).put(unauthorizeAgentRequest, LocatorType.ID.getPrefix() + agentId);

        Assertions.assertThat(checkAgentIsAuthorized(agentId)).isFalse();

        var authorizeAgentResponse = new ValidatedCrudRequester<AuthorizeAgentResponse>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.AGENTS_AUTHORIZED_INFO,
                ResponseSpec.returnsOk()
        ).put(authorizeAgentRequest, LocatorType.ID.getPrefix() + agentId);

        Assertions.assertThat(authorizeAgentResponse.isStatus()).isTrue();

        var authorizedAgent = new ValidatedCrudRequester<Agent>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.AGENTS_WITH_LOCATOR,
                ResponseSpec.returnsOk()
        ).get(LocatorType.ID.getPrefix() + agentId);

        softly.assertThat(authorizedAgent.isAuthorized()).isEqualTo(authorizeAgentResponse.isStatus());
        softly.assertThat(authorizedAgent.getAuthorizedInfo().isStatus()).isEqualTo(authorizeAgentResponse.isStatus());
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void agentCanBeUnauthorized() {
        var agentId = UserSteps.getAgentId();
        var authorizeAgentRequest = RandomGenerator.generate(AuthorizeAgentRequest.class);
        var unauthorizeAgentRequest = RandomGenerator.generate(UnauthorizeAgentRequest.class);

        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.AGENTS_AUTHORIZED_INFO,
                ResponseSpec.returnsOk()
        ).put(authorizeAgentRequest, LocatorType.ID.getPrefix() + agentId);

        Assertions.assertThat(checkAgentIsAuthorized(agentId)).isTrue();

        var authorizeAgentResponse = new ValidatedCrudRequester<AuthorizeAgentResponse>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.AGENTS_AUTHORIZED_INFO,
                ResponseSpec.returnsOk()
        ).put(unauthorizeAgentRequest, LocatorType.ID.getPrefix() + agentId);

        Assertions.assertThat(authorizeAgentResponse.isStatus()).isFalse();

        var unauthorizedAgent = new ValidatedCrudRequester<Agent>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.AGENTS_WITH_LOCATOR,
                ResponseSpec.returnsOk()
        ).get(LocatorType.ID.getPrefix() + agentId);

        softly.assertThat(unauthorizedAgent.isAuthorized()).isEqualTo(authorizeAgentResponse.isStatus());
        softly.assertThat(unauthorizedAgent.getAuthorizedInfo().isStatus()).isEqualTo(authorizeAgentResponse.isStatus());
    }

    private static boolean checkAgentIsAuthorized(int agentId) {
        return new ValidatedCrudRequester<Agent>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.AGENTS_WITH_LOCATOR,
                ResponseSpec.returnsOk()
        ).get(LocatorType.ID.getPrefix() + agentId).isAuthorized();
    }
}
