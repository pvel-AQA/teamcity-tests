package api.steps;

import api.models.Role;
import api.models.Roles;
import api.models.user.UserRequest;
import api.models.user.UserResponse;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.ValidatableCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import common.configs.Config;

import java.util.List;

import static common.configs.Config.ADMIN_PASSWORD;
import static common.configs.Config.ADMIN_USERNAME;

public class SuperUserSteps {

    public static UserResponse createAdmin() {
        return createUser(UserRequest.builder()
                .username(Config.getProperty(ADMIN_USERNAME))
                .password(Config.getProperty(ADMIN_PASSWORD))
                .roles(Roles.builder()
                        .role(List.of(Role.builder()
                                .build()))
                        .build())
                .build());
    }

    public static UserResponse createUser(UserRequest user) {
        return new ValidatableCrudRequester<UserResponse>(
                RequestSpec.superUserSpec(),
                Endpoint.USERS,
                ResponseSpec.isOk()
        ).post(user);
    }

}
