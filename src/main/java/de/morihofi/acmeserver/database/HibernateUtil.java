package de.morihofi.acmeserver.database;

import de.morihofi.acmeserver.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;

import jakarta.persistence.*;

import java.util.Locale;

/**
 * Utility class for Hibernate configuration and session management.
 */
public class HibernateUtil {
    private static SessionFactory sessionFactory;

    /**
     * Logger
     */
    private static final Logger log = LogManager.getLogger(HibernateUtil.class);

    /**
     * Initializes the database connection and Hibernate configuration.
     * This method should be called once during application startup.
     */
    public static void initDatabase() {
        if (sessionFactory == null) {
            DatabaseType dbType = switch (Main.appConfig.getDatabase().getEngine().toLowerCase(Locale.ROOT)) {
                case "h2" -> DatabaseType.H2;
                case "mariadb" -> DatabaseType.MARIADB;
                default ->
                        throw new IllegalArgumentException("Unknown or unsupported database engine: " + Main.appConfig.getDatabase().getEngine());
            };

            try {
                Configuration configuration = getConfigurationFor(dbType);

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



            Runtime.getRuntime().addShutdownHook(new Thread(){
                @Override
                public void run() {
                    this.setName("Database Shutdown Thread");
                    super.run();

                    log.info("Initiating shutdown of Hibernate Database");

                    try {
                        sessionFactory.close();
                        log.info("Shutdown of Hibernate Database completed successfully.");
                    } catch (Exception e) {
                        log.error("An error occurred during the shutdown of the Hibernate Database: {}", e.getMessage(), e);
                    }
                }
            });

        }
    }

    /**
     * Retrieves a {@link Configuration} object configured for a specific {@link DatabaseType}. This
     * method configures the database connection properties based on the type of database specified
     * by {@code dbType}. It sets properties such as the JDBC driver, connection URL, dialect, as well
     * as user credentials and additional Hibernate settings.
     * <p>
     * The method supports configuration for H2 and MariaDB databases. Depending on the {@code dbType},
     * it sets the appropriate JDBC driver, URL format, and Hibernate dialect. Common settings like
     * username, password, SQL logging, schema auto-update, and lazy loading are configured for all
     * database types.
     * <p>
     * Note: The database name, host, user, and password are obtained from the application's main
     * configuration, accessed via {@code Main.appConfig.getDatabase()}.
     *
     * @param dbType The {@link DatabaseType} enum indicating the type of database for which the
     *               configuration is to be created.
     * @return A {@link Configuration} object with properties set according to the specified
     * {@code dbType} and the application's main configuration.
     * @throws NullPointerException if {@code dbType} is null.
     */
    private static Configuration getConfigurationFor(DatabaseType dbType) {
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
        if (Main.debug) {
            //Show verbose SQL only on debug
            configuration.setProperty(Environment.SHOW_SQL, "true");
        }
        configuration.setProperty(Environment.HBM2DDL_AUTO, "update");

        configuration.setProperty(Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true");
        return configuration;
    }

    /**
     * Get the Hibernate SessionFactory for database operations.
     *
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
