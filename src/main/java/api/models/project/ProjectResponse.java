package api.models.project;

import api.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse extends BaseModel {

    private String id;
    private String name;
    private String parentProjectId;
    private String description;
    private boolean virtual;
    private String href;
    private String webUrl;
    @JsonProperty("parentProject")
    private ParentProject parentProject;
    @JsonProperty("buildTypes")
    private BuildTypesContainer buildTypes;
    @JsonProperty("templates")
    private TemplatesContainer templates;
    @JsonProperty("deploymentDashboards")
    private DeploymentDashboards deploymentDashboards;
    @JsonProperty("parameters")
    private ParametersContainer parameters;
    @JsonProperty("vcsRoots")
    private VcsRoots vcsRoots;
    @JsonProperty("projectFeatures")
    private ProjectFeatures projectFeatures;
    @JsonProperty("projects")
    private ProjectsContainer projects;

}
