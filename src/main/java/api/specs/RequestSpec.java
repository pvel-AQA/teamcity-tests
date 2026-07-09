package api.specs;

import api.models.user.UserRequest;
import com.github.dockerjava.api.exception.NotFoundException;
import common.configs.Config;
import helpers.DockerLogsExtractor;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.util.List;
import java.util.regex.Matcher;

public class RequestSpec {

    private static String superUserAuthToken;

    private RequestSpec() {
    }

    private static RequestSpecBuilder defaultSpec() {
        return new RequestSpecBuilder()
                .setAccept(ContentType.JSON)
                .setContentType(ContentType.JSON)
                .addFilters(List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()))
                .setBaseUri(Config.getProperty("apiBaseUrl"));
    }

    public static RequestSpecification userSpec() {
        return defaultSpec().build();
    }

    public static RequestSpecification superUserSpec() {
        return defaultSpec()
                .setAuth(RestAssured.basic("", getSuperUserAuthToken()))
                .build();
    }

    public static RequestSpecification authAsUserSpec(String username, String password) {
        return defaultSpec()
                .setAuth(RestAssured.basic(username, password))
                .build();
    }

    private static String getSuperUserAuthToken() {
        if (superUserAuthToken == null) {
            Matcher matcher;
            try {
                matcher = new DockerLogsExtractor().extractByPatternFromContainer(
                        DockerLogsExtractor.SUPER_USER_AUTH_TOKEN_PATTERN, Config.TEAMCITY_SERVER_NAME, 100);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (matcher.find()) {
                superUserAuthToken = matcher.group(1);
                return superUserAuthToken;
            }
            throw new NotFoundException("Super user token not found in docker logs");
        }
        return superUserAuthToken;
    }

}
