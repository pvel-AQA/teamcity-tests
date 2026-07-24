package api.enums.build;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BuildStatusText {

    CANCELED("Canceled");

    private final String value;
}
