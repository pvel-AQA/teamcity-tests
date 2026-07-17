package api.models.build;

import api.models.BaseModel;
import api.models.agent.Agent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single build, as returned by /buildQueue (when queued) and /builds/{locator} (once it runs/finishes).
 * state:  queued | running | finished
 * status: SUCCESS | FAILURE | UNKNOWN
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Build extends BaseModel {

    private int id;
    private String buildTypeId;
    private String number;
    private String state;
    private String status;
    private String statusText;
    private String href;
    private String webUrl;
    private Agent agent;
}