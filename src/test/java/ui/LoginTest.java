package ui;

import api.models.user.UserRequest;
import api.specs.RequestSpec;
import api.steps.UserSteps;
import common.annotations.AuthUser;
import common.configs.Config;
import common.enums.UserRoles;
import common.extensions.AuthUserExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import ui.pages.LoginPage;
import ui.pages.ProjectsPage;

public class LoginTest extends BaseUiTest {

    private static int GREATER_THAN_ZERO = 0;

    private int getAgentIdUsingTCSessionCookieValue() {
        String browserSessionCookieValue = RequestSpec.getBrowserSessionCookieValue();
        return UserSteps.getAgentId(browserSessionCookieValue);
    }

    @Test
    public void superUserCanLoginWithoutUsernamePopulatedTest() {
        new LoginPage().open()
                .checkUsernameIsEmpty()
                .populatePassword(Config.SUPER_USER_AUTH_TOKEN)
                .clickLoginButton()
                .getPage(ProjectsPage.class)
                .checkHeaderIsVisible();

        var agentId = getAgentIdUsingTCSessionCookieValue();
        Assertions.assertThat(agentId).isGreaterThan(GREATER_THAN_ZERO);
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void adminUserCanLoginTest() {
        UserRequest authUserRequest = AuthUserExtension.getAuthUserRequest();

        new LoginPage().open()
                .login(authUserRequest.getUsername(), authUserRequest.getPassword())
                .getPage(ProjectsPage.class)
                .checkHeaderIsVisible();

        var agentId = getAgentIdUsingTCSessionCookieValue();
        Assertions.assertThat(agentId).isGreaterThan(GREATER_THAN_ZERO);
    }
}
