package common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum UserRoles {

    PROJECT_VIEWER("p:_Root"),
    PROJECT_DEVELOPER("p:_Root"),
    SYSTEM_ADMIN("g"),
    PROJECT_ADMIN("p:_Root"),
    NOUSER_ROLE("p:_Root"),
    USER_ROLE("p:_Root")
    ;

    private String scope;
}
