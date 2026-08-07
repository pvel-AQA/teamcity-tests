package common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BuildStepCommand {
    ECHO_HELLO_WORLD("echo 'Hello World'", "Success"),

    EXIT_WITH_ERROR("exit 1", "Exit code 1");

    private final String script;
    private final String resultOfCommand;

}
