package api.specs;

import api.request.skelethon.Endpoint;
import com.codeborne.selenide.WebDriverRunner;
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
import static io.restassured.RestAssured.given;

public class RequestSpec {

    /**
     * Name of the session cookie TeamCity issues to an authenticated browser/HTTP client.
     */
    private static final String SESSION_COOKIE_NAME = "TCSESSIONID";

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
                .setAuth(RestAssured.basic("", Config.SUPER_USER_AUTH_TOKEN))
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

    // ---------------------------------------------------------------------------------------------
    // Fast UI login — seed the browser session by fetching TeamCity's TCSESSIONID cookie via the API
    // instead of driving the multi-step login form. See ui-testing-guide.md, section 4.
    // ---------------------------------------------------------------------------------------------

    /**
     * Logs in over the REST API with HTTP Basic auth and returns the {@code TCSESSIONID} cookie
     * TeamCity issues to the (now authenticated) HTTP client. A single lightweight round-trip that
     * replaces the whole browser login flow.
     */
    public static io.restassured.http.Cookie fetchSessionCookie(String username, String password) {
        return given()
                .spec(basicAuthSpec(username, password))
                .when()
                .get(Config.API_PREFIX + Endpoint.SERVER.getUrl())
                .then()
                .spec(api.specs.ResponseSpec.requestReturnsSessionCookie())
                .extract()
                .detailedCookie(SESSION_COOKIE_NAME);
    }

    /**
     * Adapter: maps a REST Assured cookie onto a Selenium {@link org.openqa.selenium.Cookie} and
     * injects it into the live WebDriver, so the SPA treats the browser as already logged in.
     * The app must already be open (a cookie domain must exist) before this is called.
     */
    public static void setCookieInBrowser(io.restassured.http.Cookie restAssuredCookie) {
        org.openqa.selenium.Cookie.Builder builder =
                new org.openqa.selenium.Cookie.Builder(restAssuredCookie.getName(), restAssuredCookie.getValue())
                        .path(restAssuredCookie.getPath() != null ? restAssuredCookie.getPath() : "/")
                        .isHttpOnly(restAssuredCookie.isHttpOnly())
                        .isSecure(restAssuredCookie.isSecured());

        if (restAssuredCookie.getDomain() != null) {
            builder.domain(restAssuredCookie.getDomain());
        }
        if (restAssuredCookie.getExpiryDate() != null) {
            builder.expiresOn(restAssuredCookie.getExpiryDate());
        }

        WebDriverRunner.getWebDriver().manage().addCookie(builder.build());
    }

    /**
     * Reads the live {@code TCSESSIONID} value out of the browser — the bridge back from the UI
     * session to the API, e.g. to confirm the UI session is also a valid API session.
     */
    public static String getBrowserSessionCookieValue() {
        org.openqa.selenium.Cookie cookie =
                WebDriverRunner.getWebDriver().manage().getCookieNamed(SESSION_COOKIE_NAME);
        return cookie == null ? null : cookie.getValue();
    }

    /**
     * Builds an API request spec that authenticates by reusing an existing {@code TCSESSIONID}
     * (typically one read from the browser via {@link #getBrowserSessionCookieValue()}).
     */
    public static RequestSpecification authWithTcSessionId(String sessionId) {
        return defaultSpecBuilder()
                .addCookie(SESSION_COOKIE_NAME, sessionId)
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
