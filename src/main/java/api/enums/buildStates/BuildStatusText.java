package api.enums.buildStates;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BuildStatusText {
    // Просто текст с маркером .* вместо имени шага
    CANCELED("Canceled"),
    COMMAND_LINE_SIGTERM("Exit code 143");

    private final String value;
}
