package api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties
public class ProjectModel extends BaseModel{
    private String name;
    private String parentProject;
    private String parentProjectName;
}
