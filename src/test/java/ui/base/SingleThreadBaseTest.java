package ui.base;

import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

//@Execution(ExecutionMode.SAME_THREAD)
@Isolated
public class SingleThreadBaseTest extends BaseUiTest {
}
