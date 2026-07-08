package api.models;

import lombok.Getter;

@Getter
public enum ErrorMessage {
    BASIC_AUTH_FAILED("Incorrect username or password"),
    OAUTH_FAILED("Invalid authentication request or authentication scheme is not supported");

    private final String text;

    ErrorMessage(String text) {
        this.text = text;
    }

}
