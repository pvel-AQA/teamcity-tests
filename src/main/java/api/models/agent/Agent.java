package api.models.agent;

import api.models.BaseModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent extends BaseModel {
    private int id;
    private String name;
    private String typeId;
    private String href;
    private String webUrl;
}
