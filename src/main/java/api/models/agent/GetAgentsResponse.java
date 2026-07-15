package api.models.agent;

import api.models.BaseModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GetAgentsResponse extends BaseModel {
    private int count;
    private String href;
    private List<Agent> agent;
}
