package de.morihofi.acmeserver.certificate;

import java.sql.*;

public class Database {

    public static Connection getDatabaseConnection() throws SQLException {
        String host = "127.0.01";
        String database = "acmeserver";
        String user = "root";
        String password = "empty";

        return DriverManager.getConnection(
                "jdbc:mariadb://" + host + "/" + database, user, password);
    }

    public static String getDatabaseVersion() throws SQLException {

        String dbVersion = "";
        Connection conn = getDatabaseConnection();

        PreparedStatement ps = conn.prepareStatement("SELECT @@version AS version; ");

        // process the results
        ResultSet rs = ps.executeQuery();
        while ( rs.next() ) {
            dbVersion = rs.getString("version");
        }

        return dbVersion;

    }

    public static void createAccount(){

    }








}
