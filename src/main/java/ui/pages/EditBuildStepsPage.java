package ui.pages;

public class EditBuildStepsPage extends EditBuildHeaderPage {

    @Override
    public String url() {
        return "/admin/editBuildRunners.html?id=buildType:%s";
    }
}
