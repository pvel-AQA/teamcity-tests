package api.models.build;

import api.models.BaseModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body for triggering a build via POST /buildQueue: {"buildType":{"id":"<buildTypeId>"}}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BuildRequest extends BaseModel {

    private BuildType buildType;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BuildType {
        private String id;
    }
}
