package ui.elements;

import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

@Getter
public class ProjectElement extends BaseElement{

    private String projectName;

    public ProjectElement(SelenideElement element) {
        super(element);
        this.projectName = element.getText().trim();
    }

}
