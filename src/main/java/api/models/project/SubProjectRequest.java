package api.models.project;

import api.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for creating a project underneath another project.
 * <p>
 * Kept separate from {@link ProjectRequest} on purpose: {@code RandomGenerator} populates
 * <b>every</b> field of the class it is given, so adding {@code parentProject} to
 * {@link ProjectRequest} would make every {@code RandomGenerator.generate(ProjectRequest.class)}
 * call point at a random, non-existent parent.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubProjectRequest extends BaseModel {

    private String id;
    private String name;
    private ProjectLocator parentProject;

}
