package api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

/**
 * Collection wrapper returned by GET /buildTypes/{btLocator}/steps.
 * TeamCity shape: {"count": N, "step": [ ... ]}.
 */
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BuildTypeStepsList extends BaseModel {
    private Integer count;
    private List<BuildTypeStepsModel> step;
}
