package api.request.skelethon.requester;

import api.models.BaseModel;
import api.request.skelethon.Endpoint;
import api.request.skelethon.HttpRequest;
import api.request.skelethon.interfaces.CrudEndpointInterface;
import api.request.skelethon.interfaces.GetAllEndpointInterface;
import common.helpers.StepLogger;
import common.helpers.EntityStorage;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.given;

public class CrudRequester extends HttpRequest implements CrudEndpointInterface, GetAllEndpointInterface {

    public CrudRequester(RequestSpecification requestSpec, Endpoint endpoint, ResponseSpecification responseSpec) {
        super(requestSpec, endpoint, responseSpec);
    }

    @Override
    public ValidatableResponse get(Object... pathParams) {
        RequestSpecification request = prepareRequest(pathParams);
        return StepLogger.log("Get request to " + targetUrl, () -> {
            return request
                    .when()
                    .get(targetUrl)
                    .then()
                    .spec(responseSpecification);
        });
    }

    @Override
    public ValidatableResponse get(Map<String, Object> queryParams) {
        RequestSpecification request = prepareRequest(queryParams);
        return StepLogger.log("Get request to " + targetUrl, () -> {
            return request
                    .when()
                    .get(targetUrl)
                    .then()
                    .spec(responseSpecification);
        });
    }

    @Override
    public ValidatableResponse post(BaseModel model) {
        return post(model, new Object[0]);
    }

    public ValidatableResponse post(BaseModel model, Object... pathParams) {
        RequestSpecification request = prepareRequest(pathParams);
        return StepLogger.log("Post request to" + targetUrl, () -> {
            ValidatableResponse response = request
                    .body(model == null ? "" : model)
                    .when()
                    .post(targetUrl)
                    .then()
                    .spec(responseSpecification);
            extractUrlToStorage(response, resolvedUrl);
            return response;
        });
    }

    private void extractUrlToStorage(ValidatableResponse response, String targetUrl) {
        if (response.extract().contentType().contains("json")) {
            String href = response.extract().jsonPath().getString("href");
            if (href != null && !href.isEmpty()) {
                EntityStorage.addUrl(href);
            } else {
                Optional.ofNullable(response.extract().jsonPath().getString("id"))
                        .ifPresent(id -> EntityStorage.addUrl(targetUrl + "/" + id));
            }
        }
    }

    @Override
    public ValidatableResponse put(BaseModel model, Object... pathParams) {
        RequestSpecification request = prepareRequest(pathParams);
        return StepLogger.log("Put request to" + targetUrl, () -> {
            return request
                    .body(model)
                    .when()
                    .put(targetUrl)
                    .then()
                    .spec(responseSpecification);
        });
    }

    @Override
    public ValidatableResponse delete(Object... pathParams) {
        RequestSpecification request = prepareRequest(pathParams);
        return StepLogger.log("Delete request to" + targetUrl, () -> {
            ValidatableResponse response =
                    request
                            .when()
                            .delete(targetUrl)
                            .then()
                            .spec(responseSpecification);
            EntityStorage.removeUrlFromListIfExists(resolvedUrl);
            return response;
        });
    }

    public ValidatableResponse deleteMethodForStorage(String urlWithId) {
        return prepareRequest()
                .when()
                .delete(urlWithId)
                .then()
                .spec(responseSpecification);
    }

    public ValidatableResponse patch(Object... pathParams) {
        return prepareRequest(pathParams)
                .when()
                .patch(targetUrl)
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse getAll(Class<?> clazz) {
        return StepLogger.log("GetAll request to" + targetUrl, () -> {
            return given()
                    .spec(requestSpecification)
                    .when()
                    .get(endpoint.getUrl())
                    .then()
                    .spec(responseSpecification);
        });
    }

    public static class QueryBuilder {

        private final Map<String, Object> params = new HashMap<>();
        private final List<String> locatorConditions = new ArrayList<>();

        public QueryBuilder add(String key, Object value) {
            params.put(key, value);

            return this;
        }

        public Map<String, Object> build() {
            Map<String, Object> result = new HashMap<>(params);
            if (!locatorConditions.isEmpty()) {
                result.put("locator", String.join(",", locatorConditions));
            }
            return result;
        }

        public QueryBuilder locator(String locator) {
            locatorConditions.add(locator);
            return this;
        }

        public QueryBuilder fields(String fields) {
            return add("fields", fields);
        }

        public QueryBuilder locatorEqualsAuthorizedAny() {
            return locator("authorized:any");
        }

        public QueryBuilder locatorEqualsConnectedTrue() {
            return locator("connected:true");
        }

    }

}
