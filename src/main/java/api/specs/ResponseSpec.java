package api.specs;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;

public class ResponseSpec {

    private ResponseSpec() {
    }

    private static ResponseSpecBuilder defaultSpec() {
        return new ResponseSpecBuilder();
    }

    public static ResponseSpecification isOk() {
        return defaultSpec().expectStatusCode(HttpStatus.SC_OK).build();
    }

    public static ResponseSpecification isCreated() {
        return defaultSpec().expectStatusCode(HttpStatus.SC_CREATED).build();
    }

    public static ResponseSpecification isBadRequest() {
        return defaultSpec().expectStatusCode(HttpStatus.SC_BAD_REQUEST).build();
    }

    public static ResponseSpecification isUnauthorized() {
        return defaultSpec().expectStatusCode(HttpStatus.SC_UNAUTHORIZED).build();
    }

    public static ResponseSpecification isForbidden() {
        return defaultSpec().expectStatusCode(HttpStatus.SC_FORBIDDEN).build();
    }

    public static ResponseSpecification deleted() {
        return defaultSpec().expectStatusCode(HttpStatus.SC_NO_CONTENT)
                .expectBody(Matchers.is(Matchers.emptyOrNullString()))
                .build();
    }

    public static ResponseSpecification requestReturnsBadResponse(String errorKey, String errorValue) {
        return defaultSpec()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(errorKey, Matchers.hasItem(errorValue))
                .build();
    }

}
