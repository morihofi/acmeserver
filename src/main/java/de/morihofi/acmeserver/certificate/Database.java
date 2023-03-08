package de.morihofi.acmeserver.certificate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    public static Connection getDatabaseConnection() throws SQLException {
        String host = "127.0.01";
        String database = "acmeserver";
        String user = "root";
        String password = "empty";

        return DriverManager.getConnection(
                "jdbc:mariadb://" + host + "/" + database, user, password);
    }




}
