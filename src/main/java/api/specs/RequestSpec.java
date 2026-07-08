package api.specs;

import common.configs.Config;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.util.List;

public class RequestSpec {

    private RequestSpec() {
    }

    private static RequestSpecBuilder defaultSpecBuilder() {
        return new RequestSpecBuilder()
                .setAccept(ContentType.JSON)
                .setContentType(ContentType.JSON)
                .addFilters(List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()))
                .setBaseUri(Config.getProperty("apiBaseUrl"));
    }

    public static RequestSpecification userSpec() {
        return defaultSpecBuilder().build();
    }
}
