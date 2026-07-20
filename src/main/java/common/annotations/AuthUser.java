package common.annotations;

import common.enums.UserRoles;
import common.extensions.AuthUserExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(AuthUserExtension.class)
public @interface AuthUser {

    UserRoles role() default UserRoles.USER_ROLE;

    /**
     * When {@code true}, {@link AuthUserExtension} also seeds the created user's
     * {@code TCSESSIONID} cookie into the browser (via the fast API-login path), so a UI
     * test lands already logged in as this user without driving the login form.
     * <p>
     * Leave {@code false} (the default) for API-only tests — they must not open a browser.
     * Only set {@code true} on UI tests (those extending {@code BaseUiTest}).
     */
    boolean seedBrowserSession() default false;

}
