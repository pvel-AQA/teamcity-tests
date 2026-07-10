package api.request.skelethon.requester;

import api.models.BaseModel;
import api.request.skelethon.Endpoint;
import api.request.skelethon.HttpRequest;
import api.request.skelethon.interfaces.CrudEndpointInterface;
import api.request.skelethon.interfaces.GetAllEndpointInterface;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import java.util.Arrays;
import java.util.List;

public class ValidatableCrudRequester<T extends BaseModel> extends HttpRequest implements CrudEndpointInterface, GetAllEndpointInterface {

    private CrudRequester crudRequester;

    public ValidatableCrudRequester(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
        this.crudRequester = new CrudRequester(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    public T get(Object... pathParams) {
        return (T) crudRequester.get(pathParams).extract().as(endpoint.getResponseModel());
    }

    public T get() {
        return get(null);
    }

    @Override
    public T post(BaseModel model) {
        return (T) crudRequester.post(model).extract().as(endpoint.getResponseModel());
    }

    public T post(BaseModel model, Object... pathParams) {
        return (T) crudRequester.post(model, pathParams).extract().as(endpoint.getResponseModel());
    }

    @Override
    public T put(BaseModel model, Object... pathParams) {
        return (T) crudRequester.put(model, pathParams).extract().as(endpoint.getResponseModel());
    }

    @Override
    public T delete(Object... pathParams) {
        return (T) crudRequester.delete(pathParams).extract().as(endpoint.getResponseModel());
    }

//    public T delete(String id, BaseModel body) {
//        return (T) crudRequester.delete(id, body).extract().as(endpoint.getResponseModel());
//    }

    @Override
    public List<T> getAll(Class<?> clazz) {
        var d = (T[]) crudRequester.getAll(clazz).extract().as(clazz);
        return Arrays.asList(d);
    }
}
