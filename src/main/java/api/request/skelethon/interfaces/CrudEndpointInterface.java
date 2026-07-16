package api.request.skelethon.interfaces;

import api.models.BaseModel;

import java.util.Map;

public interface CrudEndpointInterface {

    Object get(Object... pathParams);
    Object get(Map<String, Object> queryParams);

    Object post(BaseModel body);

    Object put(BaseModel body, Object... pathParams);

    Object delete(Object... pathParams);

}
