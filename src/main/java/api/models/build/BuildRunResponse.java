package api.models.build;

import api.enums.build.BuildState;
import api.enums.build.BuildStatus;
import api.models.BaseModel;
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
public class BuildRunResponse extends BaseModel {
    private String id;
    private String buildTypeId;
    private BuildState state;
    private BuildStatus status;
    private String href;
    private String webUrl;
    private String statusText;

    private BuildConfigurationResponse buildType;

    private CanceledInfo canceledInfo;
}
