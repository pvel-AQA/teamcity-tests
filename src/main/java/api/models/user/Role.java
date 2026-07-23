package api.models.user;

import api.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import common.enums.UserRoles;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static common.enums.UserRoles.SYSTEM_ADMIN;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Role extends BaseModel {

    @Builder.Default
    private UserRoles roleId = SYSTEM_ADMIN;
    @Builder.Default
    private String scope = "g";

}
