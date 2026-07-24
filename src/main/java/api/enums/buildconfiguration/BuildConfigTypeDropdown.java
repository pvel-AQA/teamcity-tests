package api.enums.buildconfiguration;

import lombok.Getter;

@Getter
public enum BuildConfigTypeDropdown {
    REGULAR("Regular"),
    COMPOSITE("Composite"),
    DEPLOYMENT("Deployment"),;

    private final String value;

    BuildConfigTypeDropdown(String dropdownOption) {
        this.value = dropdownOption;
    }
}
