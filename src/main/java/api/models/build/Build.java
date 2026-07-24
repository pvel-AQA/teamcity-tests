package api.models.build;

import api.models.BaseModel;
import api.models.agent.Agent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Build extends BaseModel {

    private int id;
    private String buildTypeId;
    private String number;
    private String state;
    private String status;
    private String statusText;
    private String href;
    private String webUrl;
    private Agent agent;
}
