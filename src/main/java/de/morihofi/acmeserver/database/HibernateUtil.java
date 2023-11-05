package de.morihofi.acmeserver.database;

import de.morihofi.acmeserver.Main;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.reflections.Reflections;

import javax.persistence.Entity;

public class HibernateUtil {
    private static SessionFactory sessionFactory;

    public static SessionFactory getSessionFactory() {
        String dbDriver = "mariadb";
        switch (dbDriver) {
            case "h2":
                return getSessionFactory(DatabaseType.H2);
            case "mariadb":
                return getSessionFactory(DatabaseType.MARIADB);
            default:
                return null;
        }


    }

    private static SessionFactory getSessionFactory(DatabaseType dbType) {
        if (sessionFactory == null) {
            try {
                Configuration configuration = new Configuration();

                // Einstellungen basierend auf der gewählten Datenbank
                switch (dbType) {
                    case H2:
                        configuration.setProperty(Environment.DRIVER, "org.h2.Driver");
                        configuration.setProperty(Environment.URL, "jdbc:h2:" + Main.db_name + ";DB_CLOSE_DELAY=-1");
                        configuration.setProperty(Environment.DIALECT, "org.hibernate.dialect.H2Dialect");
                        break;
                    case MARIADB:
                        configuration.setProperty(Environment.DRIVER, "org.mariadb.jdbc.Driver");
                        configuration.setProperty(Environment.URL, "jdbc:mariadb://" + Main.db_host + "/" + Main.db_name);
                        configuration.setProperty(Environment.DIALECT, "org.hibernate.dialect.MariaDBDialect");
                        break;
                }
                configuration.setProperty(Environment.USER, Main.db_user);
                configuration.setProperty(Environment.PASS, Main.db_password);
                configuration.setProperty(Environment.SHOW_SQL, "true");
                configuration.setProperty(Environment.HBM2DDL_AUTO, "update");

                configuration.setProperty(Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true");

                // Scannen von Entity-Klassen
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
        return sessionFactory;
    }

    public enum DatabaseType {
        H2, MARIADB
    }
}
