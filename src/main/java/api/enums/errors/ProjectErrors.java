package api.enums.errors;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProjectErrors {

    PROJECT_NAME_CANNOT_BE_EMPTY("Project name cannot be empty."),
    PIPELINE_WITH_THIS_NAME_ALREADY_EXISTS("Pipeline with this name already exists:"),
    NO_PROJECT_FOUND_BY_NAME_OR_INTERNAL_EXTERNAL_ID("No project found by name or internal/external id"),
    INTERNET_SERVER_ERROR_NO_NAME_500("Cannot read field \"name\" because \"descriptor\" is null");
    ;
    private String errorMsg;

}
