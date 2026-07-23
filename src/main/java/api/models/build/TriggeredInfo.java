package api.models.build;

import api.models.user.UserResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TriggeredInfo {
    private String type;
    private String date;
    private UserResponse user;
}
