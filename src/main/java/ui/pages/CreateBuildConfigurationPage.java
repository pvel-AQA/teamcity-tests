package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.back;
import static java.lang.Thread.sleep;

public class CreateBuildConfigurationPage extends BasePage<CreateBuildConfigurationPage> {

    private final SelenideElement skipButton = $(Selectors.byText("Skip"));

    public EditProjectPage clickSkipButton() throws InterruptedException {
        int maxAttempts = 2;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                skipButton.click();
                break;
            } catch (Exception | AssertionError e) {
                if (i == maxAttempts - 1) {
                    throw e;
                }
                back();
                sleep(1000);
                getPage(ConnectVCSPage.class).clickProceedWithoutRepositoryButton();
                sleep(1000);
            }
        }
        return getPage(EditProjectPage.class);
    }


    @Override
    public String url() {
        return "";
    }
}
