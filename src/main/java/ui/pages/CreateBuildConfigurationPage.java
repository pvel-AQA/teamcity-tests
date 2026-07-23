package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class CreateBuildConfigurationPage extends BasePage<CreateBuildConfigurationPage> {

    private final SelenideElement skipButton = $(Selectors.byText("Skip"));

    public EditProjectPage skip() {
      skipButton.click();
      return getPage(EditProjectPage.class);
    }

    @Override
    public String url() {
        return "";
    }
}
