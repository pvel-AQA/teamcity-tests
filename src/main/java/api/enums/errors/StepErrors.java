package api.enums.errors;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum StepErrors {

    STEP_TYPE_CANNOT_BE_EMPTY("Created step cannot have empty 'type'");
    private final String errorMsg;

}
