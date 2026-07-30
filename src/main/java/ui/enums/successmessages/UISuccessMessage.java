package ui.enums.successmessages;

import lombok.Getter;

@Getter
public enum UISuccessMessage {

    YOUR_CHANGES_HAVE_BEEN_SAVED("Your changes have been saved."),
    BUILD_CONFIGURATION_DELETED("Build configuration \"%s\" has been removed.\n" +
            "Please note that build configuration related data (builds history, artifacts and so on) will be cleaned from the database when next clean-up process is started, see clean-up policy configuration.");

    UISuccessMessage(String message) {
        this.message = message;
    }

    private final String message;

    public String addBuildconfigNameToFormattedString(String buildConfigName) {
        return String.format(message, buildConfigName);
    }
}
