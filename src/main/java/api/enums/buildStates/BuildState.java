package api.enums.buildStates;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BuildState {
    QUEUED("queued"),
    RUNNING("running"),
    FINISHED("finished");

    // Jackson будет использовать это поле для чтения и записи JSON
    @JsonValue
    private final String value;
}
