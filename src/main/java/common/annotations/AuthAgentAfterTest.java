package common.annotations;

import common.extensions.AuthAgentAfterTestExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(AuthAgentAfterTestExtension.class)
public @interface AuthAgentAfterTest {
}
