package ui.enums.successmessages;

import lombok.Getter;

@Getter
public enum UISuccessMessage {

    YOUR_CHANGES_HAVE_BEEN_SAVED("Your changes have been saved.");

    UISuccessMessage(String message) {
        this.message = message;
    }

    private final String message;
}
