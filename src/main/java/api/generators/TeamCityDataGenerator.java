package api.generators;

import api.enums.build.BuildStepCommand;
import api.models.build.*;
import api.models.project.ProjectResponse;
import api.models.project.PropertyItem;
import api.models.user.PropertiesContainer;
import api.models.user.Property;
import api.steps.UserSteps;

import java.util.List;
import java.util.UUID;

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

//    public static BuildTypeStepsModel generateBuildConfigurationStepRequest(String name) {
//
//        return BuildTypeStepsModel.builder()
//                .name(RandomGenerator.generateString(name, 3))
//                .type("simpleRunner")
//                .build();
//    }

    public static BuildTypeStepsModel generateBuildConfigurationStepRequest(String name) {
        return generateBuildConfigurationStepRequest(name, null);
    }

    // 2. Универсальный метод: принимает имя и параметры, запечатывая внутри тип "simpleRunner"
    public static BuildTypeStepsModel generateBuildConfigurationStepRequest(String name, StepProperties properties) {
        return BuildTypeStepsModel.builder()
                .name(generateString(name, 3))
                .type("simpleRunner")
                .properties(properties)
                .build();
    }

    public static BuildTypeStepsModel generateBuildConfigurationStepRequestWithCommand(BuildStepCommand command) {
        String generatedName = generateString();
        var stepProperties = StepProperties.builder()
                .property(List.of(
                        PropertyItem.builder().name("script.content").value(command.getScript()).build(),
                        PropertyItem.builder().name("use.custom.script").value("true").build()
                ))
                .build();

        return generateBuildConfigurationStepRequest(generatedName, stepProperties);
    }

    public static BuildRunRequest generateBuildRun(String buildConfigId) {
        return BuildRunRequest.builder()
                .buildTypeId(buildConfigId)
                .build();
    }

    public static BuildCancelRequest generateBuildCancel() {
        return BuildCancelRequest.builder()
                .comment(generateString("Canceled_", 8))
                .requeue(false)
                .build();
    }

    public static BuildQueuePausedRequest generateBuildQueuePausedRequest(boolean isPaused) {
        return BuildQueuePausedRequest.builder()
                .paused(isPaused)
                .build();
    }


    public static String generateString(String prefix, int length) {
        String randomPart = UUID.randomUUID().toString().substring(0, Math.min(length, 8));
        return prefix + randomPart;
    }


    public static String generateString() {
        return generateString("", 8);
    }

    public static String generateString(int length) {
        return generateString("", length);
    }

}
