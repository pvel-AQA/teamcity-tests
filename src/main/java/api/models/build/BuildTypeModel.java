package api.models.build;

import api.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BuildTypeModel extends BaseModel {
    public String id;
    public String internalId;
    public String name;
    public Boolean templateFlag;
    public String type; //regular, composite, deployment
    public Boolean paused;
    public String uuid;
    public String description;
    public String projectName;
    public String projectId;
    public String projectInternalId;
    public String href;
    public String webUrl;
}
