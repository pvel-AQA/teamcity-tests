package common.extensions;

import api.models.UserTokenResponse;
import api.models.user.UserRequest;
import api.steps.SuperUserSteps;
import api.steps.UserSteps;
import common.annotations.AuthUser;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class AuthUserExtension implements BeforeEachCallback, AfterEachCallback {

    private static final ThreadLocal<UserRequest> user = new ThreadLocal<>();
    private static final ThreadLocal<UserTokenResponse> token = new ThreadLocal<>();

    @Override
    public void beforeEach(ExtensionContext context) {

        AuthUser annotation = context.getRequiredTestMethod().getAnnotation(AuthUser.class);
        if (annotation != null) {
            UserRequest userRequest = SuperUserSteps.createUserWithRole(annotation.role());
            user.set(userRequest);
            token.set(UserSteps.getUserToken(userRequest));
        }

    }

    @Override
    public void afterEach(ExtensionContext context) {
        SuperUserSteps.deleteUser(String.valueOf(user.get().getUsername()));
        user.remove();
        token.remove();
    }

    public static UserTokenResponse getAuthUserToken() {
        return token.get();
    }

}
