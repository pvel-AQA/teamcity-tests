package api.generators;

import api.models.build.BuildCancelRequest;
import api.models.build.BuildConfigurationRequest;
import api.models.build.BuildRunRequest;
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

    public static BuildRunRequest generateBuildRun(String buildConfigId) {
        return BuildRunRequest.builder()
                .buildTypeId(buildConfigId)
                .build();
    }

    public static BuildCancelRequest generateBuildCancel() {
        return BuildCancelRequest.builder()
                .comment(RandomGenerator.generateString("Canceled_", 8))
                .requeue(false)
                .build();
    }
}
