package ui.pages;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;
import ui.elements.ProjectElement;

import java.util.List;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverConditions.urlContaining;

public class ProjectsPage extends BasePage<ProjectsPage> {

    private static final String LOGIN_PAGE_MARKER = "login.html";

    private final SelenideElement loginUsernameField = $("#username");

    public List<ProjectElement> getProjects() {
        ElementsCollection rows = $$(".ProjectsTreeItem-module__row--h3:has([data-test-itemtype='project'])")
                .shouldHave(CollectionCondition.sizeGreaterThan(0));
        return generatePageElements(rows, ProjectElement::new);
    }

    @Override
    public String url() {
        return "/";
    }

    public ProjectsPage checkItIsCorrectPage() {
        webdriver().shouldNotHave(urlContaining(LOGIN_PAGE_MARKER));
        loginUsernameField.shouldNot(exist);
        return this;
    }
}
