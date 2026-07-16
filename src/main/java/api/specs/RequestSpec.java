package api.specs;

import com.github.dockerjava.api.exception.NotFoundException;
import com.github.viclovsky.swagger.coverage.FileSystemOutputWriter;
import com.github.viclovsky.swagger.coverage.SwaggerCoverageRestAssured;
import common.configs.Config;
import common.extensions.AuthUserExtension;
import common.helpers.DockerLogsExtractor;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.authentication.PreemptiveBasicAuthScheme;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;

import static com.github.viclovsky.swagger.coverage.SwaggerCoverageConstants.OUTPUT_DIRECTORY;

public class RequestSpec {

    private static String superUserAuthToken;

    private RequestSpec() {
    }

    private static RequestSpecBuilder defaultSpecBuilder() {
        return new RequestSpecBuilder()
                .setAccept(ContentType.JSON)
                .setContentType(ContentType.JSON)
                .addFilters(List.of(
                        new RequestLoggingFilter(),
                        new ResponseLoggingFilter(),
                        new AllureRestAssured(),
                        new SwaggerCoverageRestAssured(
                                new FileSystemOutputWriter(Paths.get("target/" + OUTPUT_DIRECTORY)))
                ))
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
        return basicAuthSpec(Config.ADMIN_USERNAME, Config.ADMIN_PASSWORD);
    }


    public static RequestSpecification adminSpec() {
        return adminSpec(Config.ADMIN_TOKEN);
    }

    public static RequestSpecification unAuth() {
        return defaultSpecBuilder().build();
    }

    public static RequestSpecification adminSpec(String token) {
        return defaultSpecBuilder()
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }

    public static RequestSpecification superUserSpec() {
        return defaultSpecBuilder()
                .setAuth(RestAssured.basic("", getSuperUserAuthToken()))
                .build();
    }

    public static RequestSpecification authAsUserSpec(String username, String password) {
        return defaultSpecBuilder()
                .setAuth(RestAssured.basic(username, password))
                .build();
    }

    public static RequestSpecification withAuthExtensionUser() {
        return defaultSpecBuilder()
                .setAuth(RestAssured.oauth2(AuthUserExtension.getAuthUserToken().getValue()))
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
