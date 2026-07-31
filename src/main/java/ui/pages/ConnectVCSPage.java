package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class ConnectVCSPage extends BasePage<ConnectVCSPage> {

    private final SelenideElement proceedWithoutRepositoryButton = $(Selectors.byText("Proceed without repository"));

    public CreateBuildConfigurationPage clickProceedWithoutRepositoryButton() {
        proceedWithoutRepositoryButton.shouldBe(Condition.visible).click();
        return getPage(CreateBuildConfigurationPage.class);
    }

    @Override
    public String url() {
        return "";
    }
}
