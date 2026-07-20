package api.user;

import api.enums.errors.UserErrors;
import api.enums.locators.LocatorType;
import api.generators.RandomGenerator;
import api.models.user.UserRequest;
import api.models.user.UserResponse;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.request.skelethon.requester.ValidatedCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import base.BaseTest;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import org.junit.jupiter.api.Test;

import static api.enums.errors.AuthErrorMessage.AUTHENTICATION_REQUIRED;
import static api.enums.errors.AuthErrorMessage.BASIC_AUTH_FAILED;

public class UserTest extends BaseTest {

    private static UserRequest generateUser() {
        return UserRequest.builder()
                .username(RandomGenerator.generateString("user", 8))
                .password(RandomGenerator.generateString())
                .email(RandomGenerator.generateString("mail", 5) + "@example.com")
                .build();
    }

    private static UserResponse createUser(UserRequest userRequest) {
        return new ValidatedCrudRequester<UserResponse>(
                RequestSpec.basicAuthSpec(),
                Endpoint.USERS,
                ResponseSpec.returnsOk())
                .post(userRequest);
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void adminCanCreateUserTest() {
        UserRequest userRequest = generateUser();

        UserResponse userResponse = createUser(userRequest);

        softly.assertThat(userResponse.getUsername())
                .as("Created user should keep the requested username")
                .isEqualTo(userRequest.getUsername());
        softly.assertThat(userResponse.getId())
                .as("Server assigns a positive id to a created user")
                .isPositive();
        softly.assertThat(userResponse.getHref())
                .as("Server populates href for a created user")
                .isNotBlank();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void adminCanCreateUserAndGetByIdTest() {
        UserRequest userRequest = generateUser();
        UserResponse createdUser = createUser(userRequest);

        UserResponse fetchedUser = new ValidatedCrudRequester<UserResponse>(
                RequestSpec.basicAuthSpec(),
                Endpoint.USERS,
                ResponseSpec.returnsOk())
                .get(LocatorType.ID.getPrefix() + createdUser.getId());

        softly.assertThat(fetchedUser.getUsername())
                .as("User fetched by id should match the created user")
                .isEqualTo(userRequest.getUsername());
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void adminCanCreateUserAndGetByUsernameTest() {
        UserRequest userRequest = generateUser();
        UserResponse createdUser = createUser(userRequest);

        UserResponse fetchedUser = new ValidatedCrudRequester<UserResponse>(
                RequestSpec.basicAuthSpec(),
                Endpoint.USERS,
                ResponseSpec.returnsOk())
                .get(LocatorType.USERNAME.getPrefix() + userRequest.getUsername());

        softly.assertThat(fetchedUser.getId())
                .as("User fetched by username should match the created user id")
                .isEqualTo(createdUser.getId());
    }

    @Test
    public void adminCanDeleteUserTest() {
        UserRequest userRequest = generateUser();
        UserResponse createdUser = createUser(userRequest);
        String userLocator = LocatorType.ID.getPrefix() + createdUser.getId();

        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.USERS,
                ResponseSpec.returnsDeleted())
                .delete(userLocator);

        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.USERS,
                ResponseSpec.returnsNotFound())
                .get(userLocator);
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void adminCannotCreateUserWithDuplicateUsernameTest() {
        UserRequest userRequest = generateUser();
        UserResponse createdUser = createUser(userRequest);

        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.USERS,
                ResponseSpec.returnsBadRequest(
                        UserErrors.CANNOT_CREATE_USER_WITH_THE_SAME_USERNAME_ALREADY_EXISTS.getErrorMsg()))
                .post(userRequest);

        UserResponse existingUser = new ValidatedCrudRequester<UserResponse>(
                RequestSpec.basicAuthSpec(),
                Endpoint.USERS,
                ResponseSpec.returnsOk())
                .get(LocatorType.USERNAME.getPrefix() + userRequest.getUsername());

        softly.assertThat(existingUser.getId())
                .as("A rejected duplicate must not replace the original user")
                .isEqualTo(createdUser.getId());
    }

    @Test
    public void adminCannotCreateUserWithoutUsernameTest() {
        UserRequest userRequest = UserRequest.builder()
                .password(RandomGenerator.generateString())
                .build();

        var response = new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.USERS,
                ResponseSpec.returnsBadRequest())
                .post(userRequest);

        softly.assertThat(response.extract().asString())
                .as("Server should explain why a user without a username is rejected")
                .isNotBlank();
    }

    @Test
    public void adminCannotGetNonExistingUserByIdTest() {
        String nonExistingUser = LocatorType.ID.getPrefix() + RandomGenerator.generateString();

        var response = new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.USERS,
                ResponseSpec.returnsNotFound())
                .get(nonExistingUser);

        softly.assertThat(response.extract().asString())
                .as("Server should return a not-found message for a missing user")
                .isNotBlank();
    }

    @Test
    public void adminCannotDeleteNonExistingUserTest() {
        String nonExistingUser = LocatorType.USERNAME.getPrefix()
                + RandomGenerator.generateString("noUser", 8);

        var response = new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.USERS,
                ResponseSpec.returnsNotFound())
                .delete(nonExistingUser);

        softly.assertThat(response.extract().asString())
                .as("Server should return a not-found message when deleting a missing user")
                .isNotBlank();
    }

    @Test
    public void cannotCreateUserWithoutAuthTest() {
        UserRequest userRequest = generateUser();

        new CrudRequester(
                RequestSpec.unAuth(),
                Endpoint.USERS,
                ResponseSpec.returnsUnauthorized(AUTHENTICATION_REQUIRED))
                .post(userRequest);

        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.USERS,
                ResponseSpec.returnsNotFound())
                .get(LocatorType.USERNAME.getPrefix() + userRequest.getUsername());
    }

    @Test
    public void cannotGetUsersWithoutAuthTest() {
        new CrudRequester(
                RequestSpec.unAuth(),
                Endpoint.USERS,
                ResponseSpec.returnsUnauthorized(AUTHENTICATION_REQUIRED))
                .get();
    }

    @Test
    public void cannotCreateUserWithInvalidBasicCredentialsTest() {
        UserRequest userRequest = generateUser();

        new CrudRequester(
                RequestSpec.basicAuthSpec(
                        RandomGenerator.generateString(),
                        RandomGenerator.generateString()),
                Endpoint.USERS,
                ResponseSpec.returnsUnauthorized(BASIC_AUTH_FAILED))
                .post(userRequest);

        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.USERS,
                ResponseSpec.returnsNotFound())
                .get(LocatorType.USERNAME.getPrefix() + userRequest.getUsername());
    }

    @Test
    @AuthUser(role = UserRoles.PROJECT_VIEWER)
    public void viewerCannotCreateUserTest() {
        UserRequest userRequest = generateUser();

        var forbiddenResponse = new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.USERS,
                ResponseSpec.returnsForbidden())
                .post(userRequest);

        softly.assertThat(forbiddenResponse.extract().asString())
                .as("Server should return an access-denied message for a viewer")
                .isNotBlank();

        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.USERS,
                ResponseSpec.returnsNotFound())
                .get(LocatorType.USERNAME.getPrefix() + userRequest.getUsername());
    }
}
