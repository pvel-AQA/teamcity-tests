package ui;

import api.specs.RequestSpec;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import common.extensions.BrowserExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ui.pages.ProjectsPage;


@ExtendWith(BrowserExtension.class)
public class UsersEnterWithoutLoginTest extends BaseUiTest {

    private void assertEntersAppWithoutLogin(String userType) {
        new ProjectsPage().open().checkItIsCorrectPage();

        softly.assertThat(RequestSpec.getBrowserSessionCookieValue())
                .as("TCSESSIONID cookie should be seeded in the browser for %s", userType)
                .isNotBlank();
    }

    @Test
    @AuthUser(role = UserRoles.PROJECT_VIEWER, seedBrowserSession = true)
    @DisplayName("project viewer enters the app without login")
    void projectViewerEntersAppWithoutLogin() {
        assertEntersAppWithoutLogin("PROJECT_VIEWER");
    }

    @Test
    @AuthUser(role = UserRoles.PROJECT_DEVELOPER, seedBrowserSession = true)
    @DisplayName("project developer enters the app without login")
    void projectDeveloperEntersAppWithoutLogin() {
        assertEntersAppWithoutLogin("PROJECT_DEVELOPER");
    }

    @Test
    @AuthUser(role = UserRoles.PROJECT_ADMIN, seedBrowserSession = true)
    @DisplayName("project admin enters the app without login")
    void projectAdminEntersAppWithoutLogin() {
        assertEntersAppWithoutLogin("PROJECT_ADMIN");
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    @DisplayName("system admin enters the app without login")
    void systemAdminEntersAppWithoutLogin() {
        assertEntersAppWithoutLogin("SYSTEM_ADMIN");
    }
}
