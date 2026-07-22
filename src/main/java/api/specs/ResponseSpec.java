package api.specs;

import api.enums.errors.AuthErrorMessage;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;

import static org.hamcrest.Matchers.*;

public class ResponseSpec {

    private ResponseSpec() {
    }

    private static ResponseSpecBuilder defaultSpecBuilder() {
        return new ResponseSpecBuilder();
    }

    public static ResponseSpecification returnsOk() {
        return defaultSpecBuilder().expectStatusCode(HttpStatus.SC_OK).build();
    }

    public static ResponseSpecification returnsCreated() {
        return defaultSpecBuilder().expectStatusCode(HttpStatus.SC_CREATED).build();
    }

    public static ResponseSpecification returnsNotFound() {
        return defaultSpecBuilder().expectStatusCode(HttpStatus.SC_NOT_FOUND).build();
    }

    public static ResponseSpecification returnsNotFound(String errorMsg) {
        return defaultSpecBuilder().expectStatusCode(HttpStatus.SC_NOT_FOUND)
                .expectBody(containsString(errorMsg))
                .build();
    }

    public static ResponseSpecification returnsBadRequest() {
        return defaultSpecBuilder().expectStatusCode(HttpStatus.SC_BAD_REQUEST).build();
    }

    public static ResponseSpecification returnsBadRequest(String errorMsg) {
        return defaultSpecBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(containsString(errorMsg))
                .build();
    }

    public static ResponseSpecification requestReturnsBadResponse(String errorKey, String errorValue) {
        return defaultSpecBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(errorKey, Matchers.hasItem(errorValue))
                .build();
    }

    public static ResponseSpecification returnsNoContent() {
        return defaultSpecBuilder().expectStatusCode(HttpStatus.SC_NO_CONTENT).build();
    }

    public static ResponseSpecification returnsInternalServerError() {
        return defaultSpecBuilder().expectStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
    }

    public static ResponseSpecification returnsUnauthorized(AuthErrorMessage error) {
        return defaultSpecBuilder()
                .expectStatusCode(HttpStatus.SC_UNAUTHORIZED)
                .expectBody(containsString(error.getText()))
                .build();
    }

    public static ResponseSpecification returnsForbidden() {
        return defaultSpecBuilder().expectStatusCode(HttpStatus.SC_FORBIDDEN).build();
    }

    public static ResponseSpecification returnsDeleted() {
        return defaultSpecBuilder().expectStatusCode(HttpStatus.SC_NO_CONTENT)
                .expectBody(Matchers.is(Matchers.emptyOrNullString()))
                .build();
    }

    public static ResponseSpecification requestReturnsSessionCookie() {
        return defaultSpecBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .expectCookie(RequestSpec.SESSION_COOKIE_NAME)
                .build();
    }
}
