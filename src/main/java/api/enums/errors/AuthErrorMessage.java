package api.enums.errors;

import lombok.Getter;

@Getter
public enum AuthErrorMessage {
    BASIC_AUTH_FAILED("Incorrect username or password"),
    AUTHENTICATION_REQUIRED("Authentication required"),
    OAUTH_FAILED("Invalid authentication request or authentication scheme is not supported");

    private final String text;

    AuthErrorMessage(String text) {
        this.text = text;
    }

}
