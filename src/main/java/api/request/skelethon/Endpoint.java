package api.request.skelethon;

import api.models.BaseModel;
import api.models.ServerInfoResponse;
import lombok.AllArgsConstructor;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum Endpoint {
    SERVER(
            "/server",
            BaseModel.class,
            ServerInfoResponse.class
    );
    private String url;
    private Class<?> requestModel;
    private Class<?> responseModel;
}
