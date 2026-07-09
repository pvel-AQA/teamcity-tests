package api.generators;

import api.models.BuildConfigurationRequest;
import api.models.project.ProjectResponse;
import api.steps.UserSteps;

public class TeamCityDataGenerator {

    public static BuildConfigurationRequest generateBuildConfigurationFor() {
        ProjectResponse project = UserSteps.createProject();
        return generateBuildConfigurationFor(project.getId());
    }

    public static BuildConfigurationRequest generateBuildConfigurationFor(String projectId) {
        var buildRequest = RandomGenerator.generate(BuildConfigurationRequest.class);
        if (buildRequest.getProject() != null) {
            buildRequest.getProject().setId(projectId);
        }
        return buildRequest;
    }
}
