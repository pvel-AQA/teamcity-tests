package api.specs;

import api.errors.AuthErrorMessage;
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

    public static ResponseSpecification isOk() {
        return defaultSpecBuilder().expectStatusCode(HttpStatus.SC_OK).build();
    }

    public static ResponseSpecification isCreated() {
        return defaultSpecBuilder().expectStatusCode(HttpStatus.SC_CREATED).build();
    }

    public static ResponseSpecification isBadRequest() {
        return defaultSpecBuilder().expectStatusCode(HttpStatus.SC_BAD_REQUEST).build();
    }

    public static ResponseSpecification isUnauthorized(AuthErrorMessage error) {
        return defaultSpecBuilder()
                .expectStatusCode(HttpStatus.SC_UNAUTHORIZED)
                .expectBody(containsString(error.getText()))
                .build();
    }

    public static ResponseSpecification isForbidden() {
        return defaultSpecBuilder().expectStatusCode(HttpStatus.SC_FORBIDDEN).build();
    }

    public static ResponseSpecification deleted() {
        return defaultSpecBuilder().expectStatusCode(HttpStatus.SC_NO_CONTENT)
                .expectBody(Matchers.is(Matchers.emptyOrNullString()))
                .build();
    }

    public static ResponseSpecification requestReturnsBadResponse(String errorKey, String errorValue) {
        return defaultSpecBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(errorKey, Matchers.hasItem(errorValue))
                .build();
    }

}
