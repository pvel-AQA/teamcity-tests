package api.request.skelethon.requester;

import api.models.BaseModel;
import api.request.skelethon.Endpoint;
import api.request.skelethon.HttpRequest;
import api.request.skelethon.interfaces.CrudEndpointInterface;
import api.request.skelethon.interfaces.GetAllEndpointInterface;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import static io.restassured.RestAssured.given;

public class CrudRequester extends HttpRequest implements CrudEndpointInterface, GetAllEndpointInterface {
    private static final String PATH_PARAM_BTLOCATOR = "btLocator";
    private static final String PATH_PARAM_STEPID = "stepId";

    public CrudRequester(RequestSpecification requestSpec, Endpoint endpoint, ResponseSpecification responseSpec) {
        super(requestSpec, endpoint, responseSpec);
    }

    public ValidatableResponse get() {
        return get(null);
    }

    @Override
    public ValidatableResponse get(Integer id) {
        var temp_id= id == null ? "" : id;
        return given()
                .spec(requestSpecification)
                .when()
                .get(endpoint.getUrl() + temp_id)
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse get(String btLocator, String stepId) {
        return given()
                .spec(requestSpecification)
                .pathParam(PATH_PARAM_BTLOCATOR, btLocator)
                .pathParam(PATH_PARAM_STEPID, stepId)
                .when()
                .get(endpoint.getUrl())
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse post(BaseModel model) {
        var body = model == null ? "" : model;
        return given()
                .spec(requestSpecification)
                .body(body)
                .when()
                .post(endpoint.getUrl())
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse post(BaseModel model, String btLocator) {
        var body = model == null ? "{}" : model;

        return given()
                .spec(requestSpecification)
                .pathParam(PATH_PARAM_BTLOCATOR, btLocator)
                .when()
                .body(body)
                .post(endpoint.getUrl())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse put(Integer id, BaseModel body) {
        var url = id == null ? "" : "/" + id;
        return given()
                .spec(requestSpecification)
                .body(body)
                .when()
                .put(endpoint.getUrl() + url)
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse put(BaseModel model, String btLocator, String stepId) {
        var body = model == null ? "{}" : model;

        return given()
                .spec(requestSpecification)
                .pathParam(PATH_PARAM_BTLOCATOR, btLocator)
                .pathParam(PATH_PARAM_STEPID, stepId)
                .when()
                .body(body)
                .put(endpoint.getUrl())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse delete(Integer id, BaseModel model) {
        var body = model == null ? "" : model;
        return given()
                .spec(requestSpecification)
                .body(body)
                .when()
                .delete(endpoint.getUrl() + "/" + id)
                .then()
                .spec(responseSpecification);
    }

    public ValidatableResponse delete(String id, BaseModel model) {
        var body = model == null ? "" : model;
        return given()
                .spec(requestSpecification)
                .pathParam(PATH_PARAM_STEPID, id)
                .body(body)
                .when()
                .delete(endpoint.getUrl() + "/" + id)
                .then()
                .spec(responseSpecification);
    }

    public ValidatableResponse delete(String parameterName, String parameterValue) {
        return given()
                .spec(requestSpecification)
                .pathParam(parameterName, parameterValue)
                .when()
                .delete(endpoint.getUrl())
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
}
