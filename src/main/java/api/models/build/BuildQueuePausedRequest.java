package api.models.build;

import api.models.BaseModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildQueuePausedRequest extends BaseModel {
    private Boolean paused;
    private String reason; // Опционально, можно передавать причину
}
