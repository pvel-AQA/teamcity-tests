package common.annotations;

import common.extensions.AuthAgentAfterTestExtension;
import common.extensions.ResumeBuildQueueAfterTestExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ResumeBuildQueueAfterTestExtension.class)
public @interface ResumeBuildQueueAfterTest {
}
