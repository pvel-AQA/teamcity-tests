package api.models;

import lombok.Data;

@Data
public class ServerInfoResponse extends BaseModel {

    private String version;
    private int versionMajor;
    private int versionMinor;
    private String buildNumber;
    private String webUrl;
    private String role;
}
