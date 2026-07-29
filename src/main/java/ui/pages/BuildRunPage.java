package ui.pages;

import api.enums.build.BuildStatus;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import common.helpers.RetryUtils;

import static com.codeborne.selenide.Selenide.$;

public class BuildRunPage extends BasePage<BuildRunPage> {

    private final SelenideElement buildStatusElement = $(Selectors.byXpath("//div[contains(@class, 'Description-module__text')]"));

    @Override
    public String url() {
        return "";
    }

    public BuildRunPage checkIsStatus(BuildStatus buildStatus) {
        RetryUtils.retry(
                "Wait until status of Build Run is correct",
                buildStatusElement::getText,
                value -> value.equalsIgnoreCase(buildStatus.getValue()),
                5,
                5000
        );
        return this;
    }

    public String getBuildRunId() {
        String currentUrl = WebDriverRunner.url();

        String[] urlParts = currentUrl.split("/");
        String buildId = urlParts[urlParts.length - 1];

        return buildId;
    }
}
