package api.specs;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;

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

    public static ResponseSpecification isUnauthorized() {
        return defaultSpecBuilder().expectStatusCode(HttpStatus.SC_UNAUTHORIZED).build();
    }

    public static ResponseSpecification isForbidden() {
        return defaultSpecBuilder().expectStatusCode(HttpStatus.SC_FORBIDDEN).build();
    }

}
