package de.morihofi.acmeserver.database;

import de.morihofi.acmeserver.Main;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.reflections.Reflections;

import jakarta.persistence.*;

import java.util.Locale;

/**
 * Utility class for Hibernate configuration and session management.
 */
public class HibernateUtil {
    private static SessionFactory sessionFactory;


    /**
     * Initializes the database connection and Hibernate configuration.
     * This method should be called once during application startup.
     */
    public static void initDatabase(){
        if (sessionFactory == null) {
            DatabaseType dbType = switch (Main.appConfig.getDatabase().getEngine().toLowerCase(Locale.ROOT)) {
                case "h2" -> DatabaseType.H2;
                case "mariadb" -> DatabaseType.MARIADB;
                default ->
                        throw new IllegalArgumentException("Unknown or unsupported database engine: " + Main.appConfig.getDatabase().getEngine());
            };

            try {
                Configuration configuration = new Configuration();

                // Settings based on the selected database
                switch (dbType) {
                    case H2 -> {
                        configuration.setProperty(Environment.DRIVER, "org.h2.Driver");
                        configuration.setProperty(Environment.URL, "jdbc:h2:" + Main.appConfig.getDatabase().getName() + ";DB_CLOSE_DELAY=-1");
                        configuration.setProperty(Environment.DIALECT, "org.hibernate.dialect.H2Dialect");
                    }
                    case MARIADB -> {
                        configuration.setProperty(Environment.DRIVER, "org.mariadb.jdbc.Driver");
                        configuration.setProperty(Environment.URL, "jdbc:mariadb://" + Main.appConfig.getDatabase().getHost() + "/" + Main.appConfig.getDatabase().getName());
                        configuration.setProperty(Environment.DIALECT, "org.hibernate.dialect.MariaDBDialect");
                    }
                }
                configuration.setProperty(Environment.USER, Main.appConfig.getDatabase().getUser());
                configuration.setProperty(Environment.PASS, Main.appConfig.getDatabase().getPassword());
                configuration.setProperty(Environment.SHOW_SQL, "true");
                configuration.setProperty(Environment.HBM2DDL_AUTO, "update");

                configuration.setProperty(Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true");

                // Scan Entity classes
                Reflections reflections = new Reflections("de.morihofi.acmeserver");
                for (Class<?> clazz : reflections.getTypesAnnotatedWith(Entity.class)) {
                    configuration.addAnnotatedClass(clazz);
                }

                StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
                        .applySettings(configuration.getProperties());
                sessionFactory = configuration.buildSessionFactory(registryBuilder.build());

            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    /**
     * Get the Hibernate SessionFactory for database operations.
     * @return The SessionFactory instance.
     */
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Enumeration of supported database types.
     */
    public enum DatabaseType {
        H2, MARIADB
    }
}
