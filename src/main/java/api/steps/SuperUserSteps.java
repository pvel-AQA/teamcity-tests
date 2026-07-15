package api.steps;

import api.generators.RandomGenerator;
import api.models.Role;
import api.models.Roles;
import api.models.user.UserRequest;
import api.models.user.UserResponse;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.request.skelethon.requester.ValidatedCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import common.configs.Config;
import common.enums.UserRoles;

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
        return new ValidatedCrudRequester<UserResponse>(
                RequestSpec.superUserSpec(),
                Endpoint.USERS,
                ResponseSpec.returnsOk()
        ).post(user);
    }

    public static UserRequest createUserWithRole(UserRoles role) {
        UserRequest user = RandomGenerator.generate(UserRequest.class);
        user.setId(null);
        user.setRoles(Roles.builder()
                .role(List.of(Role.builder()
                        .roleId(role)
                        .scope(role.getScope())
                        .build()))
                .build());
        UserResponse response = new ValidatedCrudRequester<UserResponse>(
                RequestSpec.superUserSpec(),
                Endpoint.USERS,
                ResponseSpec.returnsOk()
        ).post(user);
        user.setId(String.valueOf(response.getId()));
        return user;
    }

    public static void deleteUser(String userName) {
        new CrudRequester(
                RequestSpec.superUserSpec(),
                Endpoint.USERS,
                ResponseSpec.returnsDeleted()
        ).delete(userName);
    }

}
