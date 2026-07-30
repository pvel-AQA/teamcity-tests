package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class EditBuildHeaderPage extends BasePage<EditBuildHeaderPage> {

    protected final SelenideElement lastSelectedBuildTypeBreadcrumbs = $("li.last.selected.buildType");
    protected final SelenideElement runButton = $(Selectors.byXpath("//button[text()='Run']"));

    @Override
    public String url() {
        return "/admin/editBuild.html?id=buildType:%s";
    }

    public BuildRunPage runBuild() {
        runButton.click();
        return getPage(BuildRunPage.class);
    }
}
