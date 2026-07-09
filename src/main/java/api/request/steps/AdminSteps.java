package api.request.steps;

import api.models.BuildConfigurationRequest;
import api.models.BuildConfigurationResponse;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.request.skelethon.requester.ValidatableCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;

public class AdminSteps {

    public static void createBuildConfiguration(BuildConfigurationRequest buildConf) {
        new CrudRequester(RequestSpec.adminSpec("eyJ0eXAiOiAiVENWMiJ9.QXRlMWtSMG1JTW9hbVlJQV85Qi14OE9DaEQ1.YTU0M2M5ZjAtZWFkNS00YmI1LTlkZjItYTFmNTVkYmRlZDdm"),
                Endpoint.BUILD_TYPES, ResponseSpec.isOk())
                .post(buildConf);
    }

    public static BuildConfigurationResponse getBuilds() {
        return new ValidatableCrudRequester<BuildConfigurationResponse>(RequestSpec.adminSpec("eyJ0eXAiOiAiVENWMiJ9.QXRlMWtSMG1JTW9hbVlJQV85Qi14OE9DaEQ1.YTU0M2M5ZjAtZWFkNS00YmI1LTlkZjItYTFmNTVkYmRlZDdm"),
                Endpoint.BUILD_TYPES, ResponseSpec.isOk())
                .get();
    }
}
