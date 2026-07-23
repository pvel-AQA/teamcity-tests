package api.models.build;

import api.models.user.PropertiesContainer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildStepRequest {
    private String name;
    private String type; // для командной строки это всегда "simpleRunner"
    private PropertiesContainer properties;
}
