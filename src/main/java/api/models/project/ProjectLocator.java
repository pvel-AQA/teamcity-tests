package api.models.project;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A reference to an existing project, e.g. {@code {"locator": "id:MyProject"}}.
 * Deliberately holds a single always-populated field so it never serialises nulls.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectLocator {

    private String locator;

}
