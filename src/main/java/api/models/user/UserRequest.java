package api.models.user;

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
public class UserRequest extends BaseModel {

    private String id;
    @GeneratingRule(regex = "[a-zA-Z][a-zA-Z0-9]{7}")
    private String username;
    @GeneratingRule(regex = "[a-zA-Z][a-zA-Z0-9]{7}")
    private String password;
    @GeneratingRule(regex = "[a-zA-Z]{5,10}@(mail|gmail|yahoo)\\.(com|org|net)")
    private String email;
    private Roles roles;

}
