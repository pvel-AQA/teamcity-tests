package api.request.skelethon;

import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static common.configs.Config.API_PREFIX;
import static io.restassured.RestAssured.given;

public abstract class HttpRequest {
    protected RequestSpecification requestSpecification;
    protected Endpoint endpoint;
    protected ResponseSpecification responseSpecification;

    protected String targetUrl;

    public HttpRequest(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        this.requestSpecification = requestSpecification;
        this.endpoint = endpoint;
        this.responseSpecification = responseSpecification;
    }

    protected RequestSpecification prepareRequest(Map<String, Object> queryParams, Object... pathParams) {
        var request = given().spec(requestSpecification);
        this.targetUrl = API_PREFIX + endpoint.getUrl();

        if (queryParams != null && !queryParams.isEmpty()) {
            request.queryParams(queryParams);
        }

        if (pathParams != null && pathParams.length > 0) {
            if (endpoint.isDynamic()) {
                Map<String, Object> pathMap = new HashMap<>();
                var matcher = Pattern.compile("\\{([^}]+)\\}").matcher(targetUrl);
                int i = 0;
                while (matcher.find() && i < pathParams.length) {
                    pathMap.put(matcher.group(1), pathParams[i]);
                    i++;
                }
                request.pathParams(pathMap);
            } else {
                this.targetUrl = this.targetUrl + "/" + pathParams[0];
            }
        }

        return request;
    }

    protected RequestSpecification prepareRequest(Object... pathParams) {
        return prepareRequest(Collections.emptyMap(), pathParams);
    }
}
