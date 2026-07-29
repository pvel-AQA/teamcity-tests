package ui.pages;

import com.codeborne.selenide.Condition;

public class EditBuildTypeVcsRootsPage extends EditBuildHeaderPage {

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
