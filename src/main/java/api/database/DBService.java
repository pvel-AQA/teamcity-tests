package api.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.qameta.allure.Allure;
import org.assertj.core.api.Assertions;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.SQLException;

public class DBService {

    private static final HikariDataSource DATA_SOURCE;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("db.url");
        config.setUsername("db.login");
        config.setPassword("db.password");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);

        config.setLeakDetectionThreshold(10000);
        config.setIdleTimeout(60000);
        config.setMaxLifetime(180000);
        Allure.step("DB Config URL: " + config.getJdbcUrl());

        DATA_SOURCE = new HikariDataSource(config);
    }

    public static <T> T withConnection(ConnectionCallback<T> callback) {
        try (Connection conn = DATA_SOURCE.getConnection()) {
            Settings settings = new Settings();
            settings.setMapConstructorParameterNames(true);
            DSLContext ctx = DSL.using(conn, SQLDialect.POSTGRES, settings);
            return callback.execute(ctx);
        } catch (SQLException e) {
            Assertions.fail("SQLException: "+ e);
            return null;
        }
    }

    public static DSLContext getDslContext(Connection conn) {
        Settings settings = new Settings();
        settings.setMapConstructorParameterNames(true);
        return DSL.using(conn, SQLDialect.POSTGRES, settings);
    }

    public static Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }

    @FunctionalInterface
    public interface ConnectionCallback<T> {
        T execute(DSLContext ctx) throws SQLException;
    }

    public static void closePool() {
        if (DATA_SOURCE != null && !DATA_SOURCE.isClosed()) {
            DATA_SOURCE.close();
        }
    }
}

