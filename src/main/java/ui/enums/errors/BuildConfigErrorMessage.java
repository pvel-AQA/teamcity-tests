package ui.enums.errors;

import lombok.Getter;

@Getter
public enum BuildConfigErrorMessage {

    BUILD_CONFIG_ALREADY_EXISTS("Build configuration with name \"%s\" already exists in project: \"%s\"");

    BuildConfigErrorMessage(String message) {
        this.message = message;
    }

    private final String message;
}
