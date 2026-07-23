package api.models.build;

import api.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BuildTypeStepsModel extends BaseModel {
    private String id;
    private String name;
    private String type;
    private Boolean disabled;
    private Boolean inherited;
    private String href;
    private StepProperties properties;
    private String shortDescription;

}
