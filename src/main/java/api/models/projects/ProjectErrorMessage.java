package api.models.projects;

import lombok.Getter;

@Getter
public enum ProjectErrorMessage {
    //BASIC_AUTH_FAILED("Incorrect username or password"),
    /*
    400 - try to create project with the name that already exists

401 - POST /app/rest/projects
Auth: No Auth
Body: { "name": "X", "parentProject": { "locator": "id:_Root" } }

403 - try to access with token of restricted user

405 - "Method Not Allowed" -> creation is done on the collection
/app/rest/projects/id:MyNewProject

409 - theoretically "state conflict," but rarely emitted by TeamCity's REST API

500 - http://localhost:8111/app/rest/projects/ with no body=>
Cannot read field "name" because "descriptor" is null
     */
    INTERNET_SERVER_ERROR_NO_NAME_500("Cannot read field \"name\" because \"descriptor\" is null");

    private final String text;

    ProjectErrorMessage(String text) {
        this.text = text;
    }

}
