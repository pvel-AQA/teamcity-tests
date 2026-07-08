package api.database;

import api.models.BaseModel;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DBHelper {
    public static <T> T getEntity(Class<T> model, String tableName, long id) {
        return DBService.withConnection(ctx -> ctx.select()
                .from(tableName)
                .where("id = ?", id)
                .fetchInto(model)
                .getFirst());
    }

    public static BaseModel getModelById(long id) {
        return getEntity(BaseModel.class, "PROJECTS", id);
    }
}
