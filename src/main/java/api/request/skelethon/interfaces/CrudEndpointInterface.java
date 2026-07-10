package api.request.skelethon.interfaces;

import api.models.BaseModel;
import io.restassured.response.ValidatableResponse;

public interface CrudEndpointInterface {

    Object get(Integer id);

    Object get(String btLocator, String stepId);

    Object post(BaseModel body);

    Object post(BaseModel model, String btLocator);

    Object put(Integer id, BaseModel body);

    Object put(BaseModel model, String btLocator, String stepId);

    Object delete(Integer id, BaseModel body);

    Object delete(String id, BaseModel model);

    //Object delete(String parameterName, String parameterValue);

}
