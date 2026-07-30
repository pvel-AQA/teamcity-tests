package common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BuildStatus {
    SUCCESS("SUCCESS"),
    FAILURE("FAILURE"),
    ERROR("ERROR"),
    UNKNOWN("UNKNOWN"),
    FAILED("FAILED"),//for ui
    RUNNING("RUNNING"),//for ui
    CANCELED("CANCELED"),//for ui
    BUILD_QUEUE_WAS_PAUSED("Build queue was paused"),//for ui
    IN_QUEUE("In queue");//for ui

    @JsonValue
    private final String value;
}
