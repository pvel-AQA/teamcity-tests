package api.enums.build;

import lombok.Getter;

@Getter
public enum BuildStepCommand {
    ECHO_HELLO_WORLD("echo 'Hello World!'"),
    EXIT_WITH_ERROR("exit 1");

    private final String script;

    BuildStepCommand(String script) {
        this.script = script;
    }

}
