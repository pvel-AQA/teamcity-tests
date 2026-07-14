package base;

import common.extensions.AuthUserExtension;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(AuthUserExtension.class)
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

}
