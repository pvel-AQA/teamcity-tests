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
import java.util.Map;

public class ValidatedCrudRequester<T extends BaseModel> extends HttpRequest implements CrudEndpointInterface, GetAllEndpointInterface {

    private CrudRequester crudRequester;

    public ValidatedCrudRequester(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
        this.crudRequester = new CrudRequester(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    public T get(Object... pathParams) {
        return (T) crudRequester.get(pathParams).extract().as(endpoint.getResponseModel());
    }

    @Override
    public T get(Map<String, Object> queryParams) {
        return (T) crudRequester.get(queryParams).extract().as(endpoint.getResponseModel());
    }

    public T get() {
        return get(new Object[0]);
    }

    /*@Override
    public T get(String btLocator, String stepId){
        return (T) crudRequester.get(btLocator,stepId).extract().as(endpoint.getResponseModel());
    }
*/
    @Override
    public T post(BaseModel model) {
        return (T) crudRequester.post(model).extract().as(endpoint.getResponseModel());
    }
    public T post(BaseModel model, Object... pathParams) {
        return (T) crudRequester.post(model, pathParams).extract().as(endpoint.getResponseModel());
    }

    /*@Override
    public T post(BaseModel body, String btLocator) {
        return (T) crudRequester.post(body, btLocator).extract().as(endpoint.getResponseModel());
    }

    @Override
    public T put(BaseModel body, String btLocator, String stepId) {
        return (T) crudRequester.put(body, btLocator, stepId).extract().as(endpoint.getResponseModel());
    }
*/
    public T put(BaseModel model, Object... pathParams) {
        return (T) crudRequester.put(model, pathParams).extract().as(endpoint.getResponseModel());
    }

   /* @Override
    public T put(BaseModel body, String stepId) {
        return (T) crudRequester.put(body, stepId).extract().as(endpoint.getResponseModel());
    }
*/
    @Override
    public T delete(Object... pathParams) {
        return (T) crudRequester.delete(pathParams).extract().as(endpoint.getResponseModel());
    }
    /*@Override
    public T delete(String id, BaseModel body) {
        return (T) crudRequester.delete(id, body).extract().as(endpoint.getResponseModel());
    }*/
    /*@Override
    public T delete(String parameterName, String parameterValue) {
        return (T) crudRequester.delete(parameterName, parameterValue).extract().as(endpoint.getResponseModel());
    }*/

    @Override
    public List<T> getAll(Class<?> clazz) {
        var d = (T[]) crudRequester.getAll(clazz).extract().as(clazz);
        return Arrays.asList(d);
    }
}
