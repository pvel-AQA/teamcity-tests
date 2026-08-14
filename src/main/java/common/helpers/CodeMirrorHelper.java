package common.helpers;

import static com.codeborne.selenide.Selenide.executeJavaScript;

public class CodeMirrorHelper {

    public static void setValue(String value) {
        executeJavaScript(
                "var cm = document.querySelector('.CodeMirror').CodeMirror; " +
                        "cm.setValue(arguments[0]);",
                value
                         );
    }

    public static String getValue() {
        return executeJavaScript(
                "return document.querySelector('.CodeMirror').CodeMirror.getValue();"
                                );
    }

}
