package common.helpers;

import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EntityStorage {

    @Getter
    private static final ThreadLocal<List<String>> endpointsToDelete =
            ThreadLocal.withInitial(CopyOnWriteArrayList::new);

    public static void init() {
        endpointsToDelete.set(new CopyOnWriteArrayList<>());
    }

    public static void addUrl(String url) {
        endpointsToDelete.get().add(url);
    }

    public static void removeUrlFromListIfExists(String url) {
        endpointsToDelete.get().remove(url);

    }

    public static void clear() {
        endpointsToDelete.get().forEach(id -> {
            new CrudRequester(
                    RequestSpec.withAuthExtensionUser(),
                    Endpoint.PROJECTS,
                    ResponseSpec.returnsDeleted()
            ).deleteMethodForStorage(id);
        });
        endpointsToDelete.remove();
    }

}
