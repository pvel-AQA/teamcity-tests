package base;

import api.models.user.UserRequest;
import common.configs.Config;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static common.configs.Config.ADMIN_PASSWORD;
import static common.configs.Config.ADMIN_USERNAME;

public class BaseTest {
    protected SoftAssertions softy;

    @BeforeEach
    public void beforeEach() {
        this.softy = new SoftAssertions();
    }

    @AfterEach
    public void afterEach() {
        this.softy.assertAll();
    }

    public UserRequest getAdminUser() {
        return UserRequest.builder()
                .username(Config.getProperty(ADMIN_USERNAME))
                .password(Config.getProperty(ADMIN_PASSWORD))
                .build();
    }

}
