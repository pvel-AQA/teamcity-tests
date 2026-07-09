package api.models.user;

import api.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse extends BaseModel {

    private String username;
    private int id;
    private String href;
    @JsonProperty("properties")
    private PropertiesContainer properties;
    @JsonProperty("roles")
    private RolesContainer roles;
    @JsonProperty("groups")
    private GroupsContainer groups;

}
