package ui;

import api.specs.RequestSpec;
import common.annotations.AdminSession;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ui.pages.ProjectsPage;

/**
 * Verifies that users of every type land on an authenticated screen through the fast
 * API-login path (a seeded {@code TCSESSIONID} cookie) — i.e. they "enter the app"
 * without ever driving the login form.
 *
 * <p>Role-based users are provisioned and torn down by {@link AuthUser} with
 * {@code seedBrowserSession = true}: the extension creates the user, seeds its
 * {@code TCSESSIONID} into the browser, and deletes the user afterwards. Admin uses the
 * dedicated {@link AdminSession} path.
 */
public class UsersEnterWithoutLoginTest extends BaseUiTest {

    /** Opens the app and asserts we are on an authenticated screen with a live session cookie. */
    private void assertEntersAppWithoutLogin(String userType) {
        new ProjectsPage().open().checkItIsCorrectPage();

        softly.assertThat(RequestSpec.getBrowserSessionCookieValue())
                .as("TCSESSIONID cookie should be seeded in the browser for %s", userType)
                .isNotBlank();
    }

    @Test
    @AdminSession
    @DisplayName("admin enters the app without login")
    void adminEntersAppWithoutLogin() {
        assertEntersAppWithoutLogin("admin");
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
