package common.helpers;

import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.specs.RequestSpec;
import io.restassured.builder.ResponseSpecBuilder;
import lombok.Getter;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

public class EntityStorage {

    @Getter
    private static final ThreadLocal<Deque<String>> endpointsToDelete =
            ThreadLocal.withInitial(ConcurrentLinkedDeque::new);

    private static final ThreadLocal<Map<String, Object>> createdEntities =
            ThreadLocal.withInitial(HashMap::new);

    private EntityStorage() {
    }


    public static void init() {
        endpointsToDelete.set(new ConcurrentLinkedDeque<>());
        if (createdEntities.get() == null) {
            createdEntities.set(new HashMap<>());
        }
    }

    public static void addUrl(String url) {
        endpointsToDelete.get().add(url);
    }

    public static void removeUrlFromListIfExists(String url) {
        endpointsToDelete.get().remove(url);
    }

    public static void addEntity(String key, Object entity) {
        createdEntities.get().put(key, entity);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getEntity(String key) {
        return (T) createdEntities.get().get(key);
    }

    public static void clear() {
        String url;
        while ((url = endpointsToDelete.get().pollLast()) != null) {
            try {
                new CrudRequester(
                        RequestSpec.superUserSpec(),
                        Endpoint.PROJECTS,
                        new ResponseSpecBuilder().build()
                ).deleteMethodForStorage(url);
            } catch (Exception e) {
                System.err.println("Failed to clean up entity at " + url + ": " + e.getMessage());
            }
        }
        endpointsToDelete.remove();
        createdEntities.remove();
    }

}
