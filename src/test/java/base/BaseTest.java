package base;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class BaseTest {
    protected SoftAssertions softy;

    @BeforeEach
    public void setUp() {
        this.softy = new SoftAssertions();
    }

    @AfterEach
    public void exit() {
        this.softy.assertAll();
    }
}
