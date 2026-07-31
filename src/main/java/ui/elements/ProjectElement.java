package ui.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;
import ui.pages.BuildConfigurationPage;
import ui.pages.SetupYourBuildPage;

import static com.codeborne.selenide.Selenide.$x;

@Getter
public class ProjectElement extends BaseElement {

    private String projectName;
    private SelenideElement projectElement;

    public ProjectElement(SelenideElement element) {
        super(element);
        this.projectName = element.getText().trim();
        this.projectElement = element.find(Selectors.byXpath(".//div[@data-test-itemtype='project']"));
    }

    public ProjectElement clickCreateUnderProjectButton(String projectId) {
        SelenideElement projectItem = element.find(Selectors.byXpath((".//a[@href='/project/%s']/ancestor::div[@data-test-itemtype='project']").formatted(projectId)));
        projectItem.hover();

        SelenideElement projectPlusButton = projectItem.find(Selectors.byXpath(".//button[@data-create-entity-button='true']"));
        retryUntilElementIsDisplayed(projectPlusButton);
        projectPlusButton.click();

        return this;
    }

    public SetupYourBuildPage clickNewBuildConfigurationButtonFromPopup() {
        SelenideElement selenideElement = $x("//div[@role='row' and contains(@id, 'new-build-configuration')]//a")
                .shouldBe(Condition.visible);

        Selenide.executeJavaScript("arguments[0].click();", selenideElement);

        return getPage(SetupYourBuildPage.class);
    }

    public ProjectElement expandProjectWithProjectIdIfRequired(String projectId) {
        projectElement.shouldBe(Condition.visible);
        SelenideElement specificProjectToClick = projectElement.find(Selectors.byXpath(".//a[@href='/project/%s']"
                .formatted(projectId)));

        boolean needToBeExpanded = specificProjectToClick.find(Selectors.byXpath("./preceding-sibling::button"))
                .getAttribute("data-test").equals("expand-button");

        if (needToBeExpanded) {
            specificProjectToClick.click();
        }

        return this;
    }

    public BuildConfigurationPage clickBuildConfigurationWithBuildConfigId(String buildConfigId) {
        $x("//div[@data-test-itemtype='buildType']//a[@href='/buildConfiguration/%s']"
                .formatted(buildConfigId)).shouldBe(Condition.visible)
                .click();

        return getPage(BuildConfigurationPage.class);
    }
}
