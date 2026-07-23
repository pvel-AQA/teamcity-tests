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
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // Чтобы тест не падал, если TeamCity пришлет лишние поля
public class BuildCancelResponse extends BaseModel {
//    private String id;
//    private String buildTypeId;
//    private String number;
//    private String status;         // Будет "UNKNOWN" или "FAILURE"
//    private String state;          // Будет "running" или "finished"
//    private String statusText;     // Будет "Canceled"
//    private String href;
//    private String webUrl;

    private BuildRunResponse buildRunResponse;

    // Вложенные объекты из JSON
    private CanceledInfo canceledInfo;
    private Agent agent;
    private TriggeredInfo triggered;
}
