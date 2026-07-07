package api.request.skelethon;

import api.models.BaseModel;
import lombok.AllArgsConstructor;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum Endpoint {
    AUTH(
            "test",
            BaseModel.class,
            BaseModel.class
    );
    private String url;
    private Class<?> requestModel;
    private Class<?> responseModel;
}
