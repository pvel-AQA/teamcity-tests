package common.annotations;

import common.extensions.AdminSessionExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a UI test that should start already logged in as admin.
 * <p>
 * {@link AdminSessionExtension} seeds the admin {@code TCSESSIONID} cookie in {@code @BeforeEach}
 * (via the fast API-login path), so the test lands on an authenticated screen without ever driving
 * the login form. Do <b>not</b> add this to tests that exercise the login form itself.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(AdminSessionExtension.class)
public @interface AdminSession {
}
