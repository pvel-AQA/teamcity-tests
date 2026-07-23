package api.models.build;

import api.enums.buildStates.BuildState;
import api.enums.buildStates.BuildStatus;
import api.enums.buildStates.BuildStatusText;
import api.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BuildRunResponse extends BaseModel {
    private String id;
    private String buildTypeId;
    private BuildState state;
    private BuildStatus status; // Поле пригодится, когда будем запрашивать инфо о готовом билде
    private String href;
    private String webUrl;

    private String statusText;

    // Реюзаем твой существующий DTO для вложенного объекта!
    private BuildConfigurationResponse buildType;

    private CanceledInfo canceledInfo;
}
