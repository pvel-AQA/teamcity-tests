package ui.pages.buildsteps;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import common.enums.PowerShellOptions;
import common.helpers.CodeMirrorHelper;
import lombok.Getter;
import ui.models.PowerShellUiModel;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;

@Getter
public class PowerShellStepPage extends BuildStepsPage {

    private final SelenideElement saveBtn = $(Selectors.byValue("Save"));
    private final SelenideElement buildStepNameFieldInput = $(Selectors.byId("buildStepName"));
    private final SelenideElement powerShellFieldOption = $(Selectors.byId("powershell_option"));
    private final SelenideElement stepIdFieldInput = $(Selectors.byId("newRunnerId"));
    private final SelenideElement scriptFileFieldInput = $(Selectors.byId("jetbrains_powershell_script_file"));
    private final SelenideElement runStepWithinContainerFieldInput = $(Selectors.byId("plugin.docker.imageId"));
    private final SelenideElement codeMirrorFieldForCodeOption = $x("//div[@class='CodeMirror-scroll']");

    @Override
    public String url() {
        return "";
    }

    public PowerShellStepPage addBuildStep(PowerShellUiModel model) {
        editStepNameField(model.getStepName());
        sendKeysIfNotNull(stepIdFieldInput, model.getStepId());
        setPowerShellOptionAndValue(model);
        sendKeysIfNotNull(runStepWithinContainerFieldInput, model.getRunStepWithinContainer());
        saveBuildStep();
        return this;
    }

    public PowerShellStepPage setPowerShellOptionAndValue(PowerShellUiModel model) {
        powerShellFieldOption.shouldBe(visible).selectOptionByValue(model.getScript().name());
        if (model.getScript().equals(PowerShellOptions.CODE)) {
            enterPowerShellScriptContent(model.getScriptSource());
        }
        if (model.getScript().equals(PowerShellOptions.FILE)) {
            sendKeysIfNotNull(scriptFileFieldInput, model.getScriptFile() + ".ps1");
        }
        return this;
    }

    public PowerShellStepPage enterPowerShellScriptContent(String value) {
        codeMirrorFieldForCodeOption.shouldBe(visible);
        CodeMirrorHelper.setValue(value);
        return this;
    }

    public PowerShellStepPage saveBuildStep() {
        saveBtn.shouldBe(visible).click();
        return this;
    }

    public PowerShellStepPage editStepNameField(String stepName) {
        sendKeysIfNotNull(buildStepNameFieldInput, stepName);
        return this;
    }

}
