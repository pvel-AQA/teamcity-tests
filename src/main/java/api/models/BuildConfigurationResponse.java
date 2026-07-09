package api.models;

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
    private ProjectInfo project;
    private SettingsWrapper settings;

    private Integer count;
    private String href;
    private List<BuildConfigurationResponse> buildType;


    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProjectInfo {
        private String id;
        private String name;
        private String parentProjectId;
        private String description;
        private String href;
        private String webUrl;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SettingsWrapper {
        private Integer count;
        private List<PropertyItem> property;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PropertyItem {
        private String name;
        private String value;
    }
}

