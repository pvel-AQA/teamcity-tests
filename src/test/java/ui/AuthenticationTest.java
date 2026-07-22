package ui;

import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import common.annotations.AdminSession;
import org.junit.jupiter.api.Test;
import ui.pages.ProjectsPage;

public class AuthenticationTest extends BaseUiTest {

    @Test
    @AdminSession
    void adminSessionSeedsAuthenticatedBrowserTest() {
        new ProjectsPage().open().checkItIsCorrectPage();

        String browserSessionId = RequestSpec.getBrowserSessionCookieValue();
        softly.assertThat(browserSessionId)
                .as("TCSESSIONID cookie should be present in the browser after @AdminSession")
                .isNotBlank();

        new CrudRequester(RequestSpec.authWithTcSessionId(browserSessionId),
                Endpoint.SERVER,
                ResponseSpec.returnsOk())
                .get();
    }

}
