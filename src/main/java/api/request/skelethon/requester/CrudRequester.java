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

    public CrudRequester(RequestSpecification requestSpec, Endpoint endpoint, ResponseSpecification responseSpec) {
        super(requestSpec, endpoint, responseSpec);
    }

    @Override
    public ValidatableResponse get(Integer id) {
        return given()
                .spec(requestSpecification)
                .when()
                .get(endpoint.getUrl() + (id == null ? "" : "/" + id))
                .then()
                .spec(responseSpecification);
    }

    public ValidatableResponse get() {
        return get(null);
    }

    @Override
    public ValidatableResponse post(BaseModel model) {
        return given()
                .spec(requestSpecification)
                .body(model == null ? "" : model)
                .when()
                .post(endpoint.getUrl())
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse put(Integer id, BaseModel model) {
        return given()
                .spec(requestSpecification)
                .body(model)
                .when()
                .put(endpoint.getUrl() + (id == null ? "" : "/" + id))
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse delete(Integer id, BaseModel model) {
        return given()
                .spec(requestSpecification)
                .body(model)
                .when()
                .post(endpoint.getUrl() + (id == null ? "" : "/" + id))
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
