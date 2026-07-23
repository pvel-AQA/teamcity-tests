package ui.pages;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.ElementsCollection;
import ui.elements.ProjectElement;

import java.util.List;

import static com.codeborne.selenide.Selenide.$$;

public class EditProjectPage extends BasePage<EditProjectPage> {

    public List<ProjectElement> getProjects() {
        ElementsCollection rows = $$(".ProjectsTreeItem-module__row--h3:has([data-test-itemtype='project'])")
                .shouldHave(CollectionCondition.sizeGreaterThan(0));
        return generatePageElements(rows, ProjectElement::new);
    }

    @Override
    public String url() {
        return "/";
    }
}
