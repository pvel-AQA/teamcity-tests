package ui.pages;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import ui.elements.BaseElement;

import java.util.List;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public abstract class BasePage<T extends BasePage> {


    public abstract String url();

//    public abstract T waitForLoadPage();

    public T open() {
        return Selenide.open(url(), (Class<T>) this.getClass());
    }

    public T getPage(Class<T> pageClass) {
        return Selenide.page(pageClass);
    }

    public <T extends BaseElement> List<T> generatePageElements(ElementsCollection elementsCollection, Function<SelenideElement, T> constructor) {
        return elementsCollection.stream().map(constructor).toList();
    }
}
