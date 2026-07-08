package api.specs;

import common.configs.Config;
import io.restassured.RestAssured;
import io.restassured.authentication.PreemptiveBasicAuthScheme;
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

    public static RequestSpecification basicAuthSpec(String username, String password) {
        PreemptiveBasicAuthScheme authScheme = new PreemptiveBasicAuthScheme();
        authScheme.setUserName(username);
        authScheme.setPassword(password);

        return defaultSpecBuilder()
                .setAuth(authScheme)
                .build();
    }

    public static RequestSpecification basicAuthSpec() {
        return basicAuthSpec(Config.getProperty(Config.ADMIN_USERNAME), Config.getProperty(Config.ADMIN_PASSWORD));
    }


    public static RequestSpecification bearerSpec() {
        return bearerSpec(Config.getProperty(Config.ADMIN_TOKEN));
    }

    public static RequestSpecification bearerSpec(String token) {
        return defaultSpecBuilder()
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }
}
