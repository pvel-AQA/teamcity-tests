package api.models.projects;

import api.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties
public class ProjectsResponse extends BaseModel {
    private ParentProject parentProject;
    private BuildTypes buildTypes;
    private Templates templates;
    private DeploymentDashboards deploymentDashboards;
    private Parameters parameters;
    private VcsRoots vcsRoots;
    private ProjectFeatures projectFeatures;
    private Projects projects;
    private String id;
    private String name;
    private String parentProjectId;
    private boolean virtual;
    private String description;
    private String href;
    private String webUrl;
}

