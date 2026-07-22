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

    boolean seedBrowserSession() default false;

}
