package api;

import api.generators.RandomGenerator;
import api.models.ErrorMessage;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import common.configs.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class AuthorizationUserTest {

    @Test
    public void testBasicAuthAccess() {
        new CrudRequester(RequestSpec.basicAuthSpec(),
                Endpoint.SERVER,
                ResponseSpec.isOk())
                .get();
    }

    public static Stream<Arguments> userInvalidData() {
        String validUser = Config.getProperty("admin.username");
        String validPass = Config.getProperty("admin.password");

        return Stream.of(
                Arguments.of("wrong password", validUser, RandomGenerator.generateString()),
                Arguments.of("wrong username", RandomGenerator.generateString(), validPass),
                Arguments.of("all wrong", RandomGenerator.generateString(), RandomGenerator.generateString())
        );
    }

    @MethodSource("userInvalidData")
    @ParameterizedTest(name = "Negative tests: {0}")
    public void testBasicAuthInvalidData(String description, String username, String password) {
        new CrudRequester(RequestSpec.basicAuthSpec(username, password),
                Endpoint.SERVER,
                ResponseSpec.isUnauthorized(ErrorMessage.BASIC_AUTH_FAILED))
                .get();
    }

    @Test
    public void testOAuthAccess() {
        new CrudRequester(
                RequestSpec.oauthSpec(),
                Endpoint.SERVER,
                ResponseSpec.isOk())
                .get();
    }

    @Test
    public void testOAuthInvalidToken() {
        new CrudRequester(
                RequestSpec.oauthSpec(RandomGenerator.generateString()),
                Endpoint.SERVER,
                ResponseSpec.isUnauthorized(ErrorMessage.OAUTH_FAILED))
                .get();
    }
}
