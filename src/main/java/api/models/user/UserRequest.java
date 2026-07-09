package api.models.user;

import api.models.BaseModel;
import api.models.Roles;
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
public class UserRequest extends BaseModel {

    private String id;
    private String username;
    private String password;
    private String email;
    private Roles roles;

}
