package ui.enums.errors;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProjectValidationError {

    PROJECT_NAME_CANNOT_BE_EMPTY("The field can't be empty"),
    INVALID_PROJECT_ID("Invalid project ID. IDs must start with a Latin letter, be no longer than 225 characters, and contain only Latin letters, numbers, and underscores."),
    PROJECT_WITH_THIS_NAME_ALREADY_EXISTS("A project with this name already exists"),
    PROJECT_WITH_THIS_ID_ALREADY_EXIESTS("A project with this ID already exists");

    private final String errorMsg;

}
