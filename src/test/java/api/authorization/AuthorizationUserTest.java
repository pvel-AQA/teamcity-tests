package api.authorization;

import api.generators.RandomGenerator;
import api.errors.AuthErrorMessage;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static common.configs.Config.ADMIN_PASSWORD;
import static common.configs.Config.ADMIN_USERNAME;

public class AuthorizationUserTest {

    @Test
    public void basicAuthTest() {
        new CrudRequester(RequestSpec.basicAuthSpec(),
                Endpoint.SERVER,
                ResponseSpec.isOk())
                .get();
    }

    public static Stream<Arguments> userInvalidData() {

        return Stream.of(
                Arguments.of("wrong password", ADMIN_USERNAME, RandomGenerator.generateString()),
                Arguments.of("wrong username", RandomGenerator.generateString(), ADMIN_PASSWORD),
                Arguments.of("all wrong", RandomGenerator.generateString(), RandomGenerator.generateString())
        );
    }

    @MethodSource("userInvalidData")
    @ParameterizedTest(name = "Negative tests: {0}")
    public void basicAuthInvalidDataTest(String description, String username, String password) {
        new CrudRequester(RequestSpec.basicAuthSpec(username, password),
                Endpoint.SERVER,
                ResponseSpec.isUnauthorized(AuthErrorMessage.BASIC_AUTH_FAILED))
                .get();
    }

    @Test
    public void bearerTokenAuthTest() {
        new CrudRequester(
                RequestSpec.adminSpec(),
                Endpoint.SERVER,
                ResponseSpec.isOk())
                .get();
    }

    @Test
    public void invalidBearerTokenAuthTest() {
        new CrudRequester(
                RequestSpec.adminSpec(RandomGenerator.generateString()),
                Endpoint.SERVER,
                ResponseSpec.isUnauthorized(AuthErrorMessage.OAUTH_FAILED))
                .get();
    }
}
