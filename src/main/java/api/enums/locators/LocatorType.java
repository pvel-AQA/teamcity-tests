package api.enums.locators;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum LocatorType {
    ID("id:"),
    NAME("name:"),
    USERNAME("username:");

    private final String prefix;

    @Override
    public String toString() {
        return this.prefix;
    }
}
