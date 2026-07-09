package api.request.skelethon;

import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;

public abstract class HttpRequest {
    protected RequestSpecification requestSpecification;
    protected Endpoint endpoint;
    protected ResponseSpecification responseSpecification;

    public HttpRequest(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        this.requestSpecification = requestSpecification;
        this.endpoint = endpoint;
        this.responseSpecification = responseSpecification;
    }

    protected RequestSpecification prepareRequest(Map<String, Object> queryParams, Object... pathParams) {
        var request = given().spec(requestSpecification);
        String url = endpoint.getUrl();

        if (queryParams != null && !queryParams.isEmpty()) {
            request.queryParams(queryParams);
        }

        if (pathParams != null && pathParams.length > 0) {
            if (endpoint.isDynamic()) {
                Map<String, Object> pathMap = new HashMap<>();
                var matcher = Pattern.compile("\\{([^}]+)\\}").matcher(url);
                int i = 0;
                while (matcher.find() && i < pathParams.length) {
                    pathMap.put(matcher.group(1), pathParams[i]);
                    i++;
                }
                request.pathParams(pathMap);
                request.basePath(url);
            } else {
                request.basePath(url + "/" + pathParams[0]);
            }
        } else {
            request.basePath(url);
        }

        return request;
    }

    protected RequestSpecification prepareRequest(Object... pathParams) {
        return prepareRequest(Collections.emptyMap(), pathParams);
    }
}
