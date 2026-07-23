package api.enums.buildStates;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BuildStatus {
    SUCCESS("SUCCESS"),
    FAILURE("FAILURE"),
    ERROR("ERROR"),
    UNKNOWN("UNKNOWN");

    @JsonValue
    private final String value;
}
