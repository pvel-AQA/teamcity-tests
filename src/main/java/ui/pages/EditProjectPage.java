package ui.pages;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import ui.elements.ProjectElement;
import ui.enums.successmessages.UISuccessMessage;

import java.util.List;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class EditProjectPage extends BasePage<EditProjectPage> {

    @Override
    public String url() {
        return "";
    }

    public List<ProjectElement> getProjects() {
        ElementsCollection rows = $$(".ProjectsTreeItem-module__row--h3:has([data-test-itemtype='project'])")
                .shouldHave(CollectionCondition.sizeGreaterThan(0));
        return generatePageElements(rows, ProjectElement::new);
    }

    public EditProjectPage checkSuccessMessageAppearsOnBuildConfigDeletion(UISuccessMessage successMessage, String buildConfigNameForFormattedString) {
        $("[id='message_buildTypeRemoved']").shouldBe(Condition.visible)
                .shouldHave(Condition.text(
                        successMessage.addBuildconfigNameToFormattedString(buildConfigNameForFormattedString)));

        return this;
    }
}
