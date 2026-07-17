package api.models.project;

import api.generators.GeneratingRule;
import api.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectRequest extends BaseModel {

    @GeneratingRule(regex = "[a-zA-Z][a-zA-Z0-9]{7}")
    private String id;
    @GeneratingRule(regex = "[a-zA-Z][a-zA-Z0-9]{7}")
    private String name;
    private String locator;
    private String description;
    private boolean copyAllAssociatedSettings;

}
