package base;

import api.steps.SuperUserSteps;
import api.steps.UserSteps;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import common.extensions.AuthUserExtension;
import common.extensions.TimingExtension;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(AuthUserExtension.class)
@ExtendWith(TimingExtension.class)
public class BaseTest {
    protected SoftAssertions softly;

    @BeforeAll
    public static void setupAgent() {
        int agentId = SuperUserSteps.getAgentId();
        SuperUserSteps.authorizeAgent(agentId);
    }

    @BeforeEach
    public void beforeEach() {
        this.softly = new SoftAssertions();
    }

    @AfterEach
    public void afterEach() {
        this.softly.assertAll();
    }

}
