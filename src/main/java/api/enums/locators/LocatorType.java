package api.enums.locators;

import lombok.AllArgsConstructor;

@AllArgsConstructor
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
