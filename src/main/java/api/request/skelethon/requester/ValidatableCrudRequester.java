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
    public T get(Integer id) {
      return (T) crudRequester.get(id).extract().as(endpoint.getResponseModel());
    }

    @Override
    public T post(BaseModel body) {
        return (T) crudRequester.post(body).extract().as(endpoint.getResponseModel());
    }

    @Override
    public T put(Integer id, BaseModel body) {
        return (T) crudRequester.put(id, body).extract().as(endpoint.getResponseModel());
    }

    @Override
    public T delete(Integer id, BaseModel body) {
        return (T) crudRequester.delete(id, body).extract().as(endpoint.getResponseModel());
    }

    @Override
    public List<T> getAll(Class<?> clazz) {
        var d = (T[]) crudRequester.getAll(clazz).extract().as(clazz);
        return Arrays.asList(d);
    }
}
