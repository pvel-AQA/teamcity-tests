package ui.pages;

import com.codeborne.selenide.ClickOptions;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import ui.enums.errors.ProjectValidationError;

import java.time.Duration;

import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class CreateProjectPage extends BasePage<CreateProjectPage> {

    private final SelenideElement projectNameInput = $(Selectors.byAttribute("data-test", "project-name-input"));

    private final SelenideElement projectIdInput = $(Selectors.byAttribute("data-test", "project-id-input"));

    private final SelenideElement projectDescriptionInput = $(Selectors.byAttribute("aria-label", "Project description"));

    private final SelenideElement createButton = $(Selectors.byText("Create"));

    private final SelenideElement cancelButton = $(Selectors.byText("Cancel"));

    private final SelenideElement projectNameError = $(Selectors.byAttribute("data-test", "project-name-error"));

    private final SelenideElement projectIdError = $(Selectors.byAttribute("data-test", "project-id-error"));


    public CreateProjectPage createProject(String projectName, String projectId, String projectDescription) {
        projectNameInput.setValue(projectName);
        projectIdInput.setValue(projectId);
        projectDescriptionInput.setValue(projectDescription);
        createButton.getWrappedElement().click();
        return this;
    }

    public ProjectsPage cancelProject() {
        cancelButton.click();
        return getPage(ProjectsPage.class);
    }

    public CreateProjectPage checkProjectNameError(ProjectValidationError error) {
        assertThat(projectNameError.getText()).isEqualTo(error.getErrorMsg());
        return this;
    }

    public CreateProjectPage checkProjectIdError(ProjectValidationError error) {
        assertThat(projectIdError.getText()).isEqualTo(error.getErrorMsg());
        return this;
    }


    @Override
    public String url() {
        return "/projects/create?projectId=_Root&cameFromUrl=/favorite/projects";
    }
}
