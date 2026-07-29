package ui.pages;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class EditBuildHeaderPage extends BasePage<EditBuildHeaderPage> {

    protected final SelenideElement lastSelectedBuildTypeBreadcrumbs = $("li.last.selected.buildType");

    @Override
    public String url() {
        return "/admin/editBuild.html?id=buildType:%s";
    }
}
