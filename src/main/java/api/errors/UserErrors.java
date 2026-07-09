package api.errors;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum UserErrors {

    CANNOT_CREATE_USER_WITH_THE_SAME_USERNAME_ALREADY_EXISTS(
            "Cannot create user as user with the same username already exists"),
    ;
    private String errorMsg;

}
