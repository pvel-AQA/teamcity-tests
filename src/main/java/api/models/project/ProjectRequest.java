package api.models.project;

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

    private String id;
    private String name;
    private String locator;
    private boolean copyAllAssociatedSettings;

}
