package de.morihofi.acmeserver.database;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.config.DatabaseConfig;
import de.morihofi.acmeserver.config.databaseConfig.JDBCUrlDatabaseConfig;
import de.morihofi.acmeserver.config.databaseConfig.OldDatabaseConfig;
import jakarta.persistence.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.reflections.Reflections;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

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
    public static void initDatabase() throws IOException {
        if (sessionFactory == null) {

            DatabaseConfig databaseConfig = Main.appConfig.getDatabase();
            JDBCUrlDatabaseConfig jdbcUrlDatabaseConfig;
            if(databaseConfig instanceof JDBCUrlDatabaseConfig jdbcDbConfig){
                jdbcUrlDatabaseConfig = jdbcDbConfig;
            }else {
                jdbcUrlDatabaseConfig = new JDBCUrlDatabaseConfig();
                if(databaseConfig instanceof OldDatabaseConfig oldDatabaseConfig){
                    log.warn("Your configuration uses the old database configuration scheme. This is deprecated. It will be automatically updated.");


                    String jdbcString = switch (oldDatabaseConfig.getEngine()){
                        case "h2" ->  "jdbc:h2:" + oldDatabaseConfig.getName() + ";DB_CLOSE_DELAY=-1";
                        case "mariadb" ->  "jdbc:mariadb://" + oldDatabaseConfig.getHost() + "/" + oldDatabaseConfig.getName();
                        case "postgres" ->  "jdbc:postgresql://" + oldDatabaseConfig.getHost() + "/" + oldDatabaseConfig.getName();
                        default ->
                                throw new IllegalStateException("Unexpected database engine: " + oldDatabaseConfig.getEngine() + ". Only h2, mariadb and postgres are valid values");
                    };

                    jdbcUrlDatabaseConfig.setJdbcUrl(jdbcString);
                    jdbcUrlDatabaseConfig.setUser(oldDatabaseConfig.getUser());
                    jdbcUrlDatabaseConfig.setPassword(oldDatabaseConfig.getPassword());


                    //Update configuration file
                    {
                        log.info("Updating server database configuration");
                        Main.appConfig.setDatabase(jdbcUrlDatabaseConfig);
                        Main.saveServerConfiguration();
                    }

                }
            }

            try {
                Configuration configuration = getConfigurationFor(jdbcUrlDatabaseConfig);

                // Scan Entity classes
                Reflections reflections = new Reflections(MethodHandles.lookup().lookupClass().getPackageName());
                for (Class<?> clazz : reflections.getTypesAnnotatedWith(Entity.class)) {
                    configuration.addAnnotatedClass(clazz);
                }

                StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
                        .applySettings(configuration.getProperties());
                sessionFactory = configuration.buildSessionFactory(registryBuilder.build());

            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }


            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    this.setName("Database Shutdown Thread");
                    super.run();

                    shutdown();
                }
            });

        }
    }

    /**
     * Retrieves a {@link Configuration} object configured for a specific {@link JDBCUrlDatabaseConfig}. This
     * method configures the database connection properties based on the type of database specified
     * by its JDBC URL. It sets properties such as the JDBC driver, connection URL, dialect, as well
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
     * @return A {@link Configuration} object with properties set according to the specified
     * {@code dbType} and the application's main configuration.
     * @throws NullPointerException if {@code dbType} is null.
     */
    private static Configuration getConfigurationFor(JDBCUrlDatabaseConfig jdbcConfig) {
        Configuration configuration = new Configuration();

        String jdbcUrl = jdbcConfig.getJdbcUrl();

        assert jdbcUrl != null;

        if (!jdbcUrl.startsWith("jdbc:")) {
            throw new IllegalArgumentException("JDBC configuration string MUST start with \"jdbc:\"");
        }

        // Database dialect and driver based on JDBC URL
        configureDialectAndDriver(configuration, jdbcUrl);

        //Use connection pool
        if(true){
            // Agroal Connection Pool settings
            configureAgroalConnectionPool(configuration);
        }else {
            // No connection pool
            configuration.setProperty("hibernate.connection.provider_class", "org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl");
        }



        log.info("Configuring JDBC URL and login credentials");
        configuration.setProperty(Environment.JAKARTA_JDBC_URL, jdbcConfig.getJdbcUrl());
        configuration.setProperty(Environment.JAKARTA_JDBC_USER, jdbcConfig.getUser());
        configuration.setProperty(Environment.JAKARTA_JDBC_PASSWORD, jdbcConfig.getPassword());

        // Enable statistics
        configuration.setProperty("hibernate.generate_statistics", "true");


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

    private static void configureDialectAndDriver(Configuration configuration, String jdbcUrl) {
        if (jdbcUrl.startsWith("jdbc:h2")) {
            configuration.setProperty(Environment.JAKARTA_JDBC_DRIVER, "org.h2.Driver");
            configuration.setProperty(Environment.DIALECT, "org.hibernate.dialect.H2Dialect");
            log.info("Detected H2 Database JDBC, using its recommended configuration");
        } else if (jdbcUrl.startsWith("jdbc:mariadb")) {
            configuration.setProperty(Environment.JAKARTA_JDBC_DRIVER, "org.mariadb.jdbc.Driver");
            configuration.setProperty(Environment.DIALECT, "org.hibernate.dialect.MariaDBDialect");
            log.info("Detected MariaDB JDBC, using its recommended configuration");
        } else if (jdbcUrl.startsWith("jdbc:postgresql")) {
            configuration.setProperty(Environment.JAKARTA_JDBC_DRIVER, "org.postgresql.Driver");
            configuration.setProperty(Environment.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
            log.info("Detected PostgreSQL JDBC, using its recommended configuration");
        } else if (jdbcUrl.startsWith("jdbc:mysql")) {
            configuration.setProperty(Environment.JAKARTA_JDBC_DRIVER, "com.mysql.cj.jdbc.Driver");
            configuration.setProperty(Environment.DIALECT, "org.hibernate.dialect.MySQLDialect");
            log.info("Detected MySQL JDBC, using its recommended configuration");
        }
    }

    private static void configureAgroalConnectionPool(Configuration configuration) {
        // Beispielhafte Agroal Konfigurationseinstellungen
        log.info("Configuring Agroal connection pool");
        configuration.setProperty("hibernate.connection.provider_class", "org.hibernate.agroal.internal.AgroalConnectionProvider");
        configuration.setProperty("hibernate.agroal.minSize", "10");
        configuration.setProperty("hibernate.agroal.maxSize", "50");
        configuration.setProperty("hibernate.agroal.initialSize", "25");
        configuration.setProperty("hibernate.agroal.maxLifetime", "PT1000S"); //Lifetime of 1000 seconds
    }


    public static void shutdown() {
        if (sessionFactory == null) {
            log.warn("Unable to shutdown Hibernate Database, cause it wasn't initialized");
            return;
        }

        log.info("Initiating shutdown of Hibernate Database");
        try {
            sessionFactory.close();
            log.info("Shutdown of Hibernate Database completed successfully.");
            log.info("Free up resources");
            sessionFactory = null;
        } catch (Exception e) {
            log.error("An error occurred during the shutdown of the Hibernate Database: {}", e.getMessage(), e);
        }
    }
}
