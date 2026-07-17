package api.request.skelethon.requester;

import api.models.BaseModel;
import api.request.skelethon.Endpoint;
import api.request.skelethon.HttpRequest;
import api.request.skelethon.interfaces.CrudEndpointInterface;
import api.request.skelethon.interfaces.GetAllEndpointInterface;
import common.helpers.EntityStorage;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.given;

public class CrudRequester extends HttpRequest implements CrudEndpointInterface, GetAllEndpointInterface {

    public CrudRequester(RequestSpecification requestSpec, Endpoint endpoint, ResponseSpecification responseSpec) {
        super(requestSpec, endpoint, responseSpec);
    }

    @Override
    public ValidatableResponse get(Object... pathParams) {
        return prepareRequest(pathParams)
                .when()
                .get(targetUrl)
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse get(Map<String, Object> queryParams) {
        return prepareRequest(queryParams)
                .when()
                .get(targetUrl)
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse post(BaseModel model) {
        return post(model, new Object[0]);
    }

    public ValidatableResponse post(BaseModel model, Object... pathParams) {
        ValidatableResponse response = prepareRequest(pathParams)
                .body(model == null ? "" : model)
                .when()
                .post(targetUrl)
                .then()
                .spec(responseSpecification);
        Optional.ofNullable(response.extract().jsonPath().getString("id"))
                .ifPresent(id -> EntityStorage.addUrl(targetUrl + "/" + id));
        return response;
    }

    @Override
    public ValidatableResponse put(BaseModel model, Object... pathParams) {
        return prepareRequest(pathParams)
                .body(model)
                .when()
                .put(targetUrl)
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse delete(Object... pathParams) {
        EntityStorage.removeUrlFromListIfExists(targetUrl);
        return prepareRequest(pathParams)
                .when()
                .delete(targetUrl)
                .then()
                .spec(responseSpecification);
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
        return given()
                .spec(requestSpecification)
                .when()
                .get(endpoint.getUrl())
                .then()
                .spec(responseSpecification);
    }

    public static class QueryBuilder {

        private final Map<String, Object> params = new HashMap<>();

        public QueryBuilder add(String key, Object value) {
            params.put(key, value);

            return this;
        }

        public Map<String, Object> build() {
            return params;
        }

        public QueryBuilder locator(String locator) {
            return add("locator", locator);
        }

        public QueryBuilder locatorEqualsAuthorizedAny() {
            return locator("authorized:any");
        }

    }

}
