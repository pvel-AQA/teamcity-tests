package api.enums.build;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BuildState {
    QUEUED("queued"),
    RUNNING("running"),
    FINISHED("finished");

    @JsonValue
    private final String value;
}
