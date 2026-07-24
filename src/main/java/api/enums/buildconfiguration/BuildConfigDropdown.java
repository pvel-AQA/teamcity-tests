package api.enums.buildconfiguration;

import lombok.Getter;

@Getter
public enum BuildConfigDropdown {
    FROM_TEMPLATE("From template"),
    WITHOUT_REPOSITORY("Without repository");

    BuildConfigDropdown(String dropdownOption) {
        this.dropdownOption = dropdownOption;
    }

    private final String dropdownOption;
}
