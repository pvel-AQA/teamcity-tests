package ui.enums.alerts;

import lombok.Getter;

@Getter
public enum BuildConfigAlert {
    DELETE_BUILD_CONFIG("Are you sure you want to delete \"%s\" build configuration and all related data?");

    BuildConfigAlert(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    private final String alertMessage;

    public String addBuildconfigNameToFormattedString(String buildConfigName) {
        return String.format(alertMessage, buildConfigName);
    }
}
