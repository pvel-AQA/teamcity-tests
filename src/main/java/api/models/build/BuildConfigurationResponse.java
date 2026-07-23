package api.models.build;

import api.models.BaseModel;
import api.models.project.ParametersContainer;
import api.models.project.ProjectResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BuildConfigurationResponse extends BaseModel {

    private String id;
    private String name;
    private String projectName;
    private String projectId;
    private String webUrl;

    private ProjectResponse project;

    private ParametersContainer settings;

    private Integer count;
    private String href;
    private List<BuildConfigurationResponse> buildType;

    private Boolean paused;
}

