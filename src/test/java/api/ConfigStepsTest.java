package api;

import api.steps.Steps;
import org.junit.jupiter.api.Test;

public class ConfigStepsTest {
    //login
    //create project
    //create config
    //create step

    //+ step was created
    //-400 - missing/empty buildType, malformed JSON, or a bad field.
    //401 - wrong token or Auth: No Auth
    //403 - try to access with token of restricted user
    //404 - send in body non-existing info
    //500 - http://localhost:8111/app/rest/buildQueue/ with no body=>
    //Cannot read field "name" because "descriptor" is null
    //-

    @Test
    public void ConfigStepsCreatedTest() {
        String projectName = Steps.createProject().getName();
        String config = Steps.createConfig(projectName).getName();

        System.out.println(projectName);
        System.out.println(config);
    }
}
