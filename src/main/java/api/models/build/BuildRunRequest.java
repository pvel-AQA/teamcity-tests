package api.models.build;

import api.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildRunRequest extends BaseModel {

    @JsonIgnore // Jackson проигнорирует само поле при сериализации
    private String buildTypeId;

    // Jackson вызовет этот метод и создаст в JSON объект {"buildType": {"id": "..."}}
    @JsonProperty("buildType")
    public Map<String, String> getBuildTypeJson() {
        if (this.buildTypeId == null) {
            return null;
        }
        return Map.of("id", this.buildTypeId);
    }
}
