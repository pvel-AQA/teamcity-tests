package api.request.skelethon.requester;

import api.models.BaseModel;
import api.request.skelethon.Endpoint;
import api.request.skelethon.HttpRequest;
import api.request.skelethon.interfaces.CrudEndpointInterface;
import api.request.skelethon.interfaces.GetAllEndpointInterface;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;

public class CrudRequester extends HttpRequest implements CrudEndpointInterface, GetAllEndpointInterface {

    public CrudRequester(RequestSpecification requestSpec, Endpoint endpoint, ResponseSpecification responseSpec) {
        super(requestSpec, endpoint, responseSpec);
    }

    @Override
    public ValidatableResponse get(Object... pathParams) {
        return prepareRequest(pathParams)
                .when()
                .get("")
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse post(BaseModel model) {
        return post(model, new Object[0]);
    }

    public ValidatableResponse post(BaseModel model, Object... pathParams) {
        return prepareRequest(pathParams)
                .body(model == null ? "" : model)
                .when()
                .post("")
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse put(BaseModel model, Object... pathParams) {
        return prepareRequest(pathParams)
                .body(model)
                .when()
                .put("")
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse delete(Object... pathParams) {
        return prepareRequest(pathParams)
                .when()
                .delete("")
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
