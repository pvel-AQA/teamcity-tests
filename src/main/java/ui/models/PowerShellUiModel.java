package ui.models;

import api.generators.GeneratingRule;
import api.models.BaseModel;
import common.enums.PowerShellOptions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PowerShellUiModel extends BaseModel {

    @GeneratingRule(regex = "[a-zA-Z][a-zA-Z0-9]{7}")
    private String stepName;
    private String stepId;
    private PowerShellOptions script;
    private String scriptSource;
    private String scriptFile;
    private String scriptExecutionMode;
    @GeneratingRule(regex = "[a-zA-Z][a-zA-Z0-9]{7}")
    private String runStepWithinContainer;

}
