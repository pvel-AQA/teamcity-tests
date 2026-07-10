package api.request.skelethon.interfaces;

import api.models.BaseModel;

public interface CrudEndpointInterface {

    public Object get(Object... pathParams);

    public Object post(BaseModel body);

    public Object put(BaseModel body, Object... pathParams);

    public Object delete(Object... pathParams);

}
