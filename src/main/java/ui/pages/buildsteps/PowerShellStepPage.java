package ui.pages.buildsteps;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import common.enums.BuildStepsRunners;
import common.enums.PowerShellOptions;
import common.helpers.CodeMirrorHelper;

import java.time.Duration;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class PowerShellStepPage extends BuildStepsPage {

    SelenideElement saveBtn = $(Selectors.byValue("Save"));
    SelenideElement buildStepNameInput = $(Selectors.byId("buildStepName"));
    SelenideElement powershellOption = $(Selectors.byId("powershell_option"));

    public PowerShellStepPage addBuildStep(String codeScript) {
        addBuildStepBtn.shouldBe(visible).click();
        newBuildStepTitle.shouldHave(appear, Duration.ofSeconds(5));
        selectRunner(BuildStepsRunners.POWER_SHELL);
        //buildStepNameInput.shouldBe(visible).sendKeys("SomeName");
        selectPowerShellOption(PowerShellOptions.CODE);
        enterPowerShellScriptContent(codeScript);
        saveBtn.shouldBe(visible).click();
        return this;
    }

    public PowerShellStepPage selectPowerShellOption(PowerShellOptions option) {
        powershellOption.shouldBe(visible).selectOptionByValue(option.name());
        return this;
    }

    public PowerShellStepPage enterPowerShellScriptContent(String value) {
        CodeMirrorHelper.setValue(value);
        return this;
    }

}
