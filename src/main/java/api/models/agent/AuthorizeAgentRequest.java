package api.models.agent;

import api.generators.GeneratingRule;
import api.models.BaseModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthorizeAgentRequest extends BaseModel {
    @GeneratingRule(regex = "^true$")
    private boolean status;
}
