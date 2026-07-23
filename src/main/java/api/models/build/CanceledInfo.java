package api.models.build;

import api.models.user.UserResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CanceledInfo {
    private UserResponse user;
    private String timestamp;
    private String text; // Сюда придет ваш комментарий "Stopped by test"
}
