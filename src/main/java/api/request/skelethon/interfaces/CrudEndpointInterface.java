package api.request.skelethon.interfaces;

import api.models.BaseModel;

public interface CrudEndpointInterface {

    public Object get(Integer id);

    public Object post(BaseModel body);

    public Object put(Integer id, BaseModel body);

    public Object delete(Integer id, BaseModel body);

}
