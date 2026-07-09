package api.models;

import api.generators.RandomGenerator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopyBuildConfigurationRequest extends BaseModel {

    private String sourceBuildTypeLocator;
    private String id;
    private String name;
    private Boolean copyAllAssociatedSettings;

    public static CopyBuildConfigurationRequest createFrom(BuildConfigurationResponse buildConf) {
        return CopyBuildConfigurationRequest.builder()
                .sourceBuildTypeLocator("id:" + buildConf.getId())
                .id("copy_" + RandomGenerator.generateString())
                .name("Copy of " + buildConf.getName())
                .copyAllAssociatedSettings(true)
                .build();
    }
}
