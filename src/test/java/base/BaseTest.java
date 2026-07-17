package base;

import common.extensions.AuthUserExtension;
import common.helpers.EntityStorage;
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
        EntityStorage.init();
    }

    @AfterEach
    public void afterEach() {
        this.softy.assertAll();
        EntityStorage.clear();
    }

}
