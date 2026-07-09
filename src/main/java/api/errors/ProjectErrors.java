package api.errors;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ProjectErrors {

    PROJECT_NAME_CANNOT_BE_EMPTY("Project name cannot be empty."),
    ;
    private String errorMsg;

}
