package api.models.agent;

import api.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Agent extends BaseModel {
    private int id;
    private String name;
    private String typeId;
    private boolean connected;
    private boolean enabled;
    private boolean authorized;
    private String href;
    private String webUrl;
    private EnabledInfo enabledInfo;
    private AuthorizedInfo authorizedInfo;
}
