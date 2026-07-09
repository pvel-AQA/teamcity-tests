package api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties
public class BuildTypeStepsModel extends BaseModel {
    private String id;
    private String name;
    private String type;
    private Boolean disabled;
    private Boolean inherited;
    private String href;
    //private Properties properties;
    private String shortDescription;

}
