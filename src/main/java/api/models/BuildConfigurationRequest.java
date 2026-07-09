package api.models;

import api.generators.GeneratingRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BuildConfigurationRequest extends BaseModel{

    @GeneratingRule(regex = "[a-zA-Z][a-zA-Z0-9]{7}")
    private String id;
    private String name;
    private Project project;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Project {
        private String id;
    }
}
