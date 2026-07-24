package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class EditBuildTypeVcsRootsPage extends BasePage<EditBuildTypeVcsRootsPage> {

    private final SelenideElement lastSelectedBuildTypeBreadcrumbs = $("li.last.selected.buildType");

    @Override
    public String url() {
        return "/admin/editBuildTypeVcsRoots.html?id=buildType:%s";
    }

    public EditBuildGeneralPage clickLastSelectedBuildTypeBreadcrumbs() {
        lastSelectedBuildTypeBreadcrumbs.shouldBe(Condition.visible);
        lastSelectedBuildTypeBreadcrumbs.click();

        return getPage(EditBuildGeneralPage.class);
    }

    public String getBuildConfigNameFromBreadcrumbs() {
        lastSelectedBuildTypeBreadcrumbs.shouldBe(Condition.visible);

        return lastSelectedBuildTypeBreadcrumbs.getText();
    }
}
