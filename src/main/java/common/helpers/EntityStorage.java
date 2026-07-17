package common.helpers;

import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.specs.RequestSpec;
import io.restassured.builder.ResponseSpecBuilder;
import lombok.Getter;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class EntityStorage {

    @Getter
    private static final ThreadLocal<Deque<String>> endpointsToDelete =
            ThreadLocal.withInitial(ConcurrentLinkedDeque::new);

    public static void init() {
        endpointsToDelete.set(new ConcurrentLinkedDeque<>());
    }

    public static void addUrl(String url) {
        endpointsToDelete.get().add(url);
    }

    public static void removeUrlFromListIfExists(String url) {
        endpointsToDelete.get().remove(url);

    }

    public static void clear() {
        String url;
        while ((url = endpointsToDelete.get().pollLast()) != null) {
            new CrudRequester(
                    RequestSpec.withAuthExtensionUser(),
                    Endpoint.PROJECTS,
                    new ResponseSpecBuilder().build()
            ).deleteMethodForStorage(url);
        }
        endpointsToDelete.remove();
    }

}
