package common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ui.pages.buildsteps.BuildStepsPage;
import ui.pages.buildsteps.PowerShellStepPage;

import java.util.function.Supplier;

@AllArgsConstructor
@Getter
public enum BuildStepsRunners {

    POWER_SHELL("PowerShell", "PowerShell", PowerShellStepPage::new),
    /*COMMAND_LINE("Command Line", "Simple command execution, null"),*/
    ;

    private final String displayName;
    private final String runnerType;
    private Supplier<? extends BuildStepsPage> pageSupplier;

    @SuppressWarnings("unchecked")
    public <T extends BuildStepsPage> T createPage() {
        return (T) pageSupplier.get();
    }

}
