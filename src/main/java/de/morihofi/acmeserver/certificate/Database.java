package de.morihofi.acmeserver.certificate;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.certificate.objects.ACMEIdentifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class Database {

    public static Logger log = LogManager.getLogger(Main.class);

    public static Connection getDatabaseConnection() throws SQLException {
        String host = "138.201.116.45:3306";
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
        while (rs.next()) {
            dbVersion = rs.getString("version");
        }

        return dbVersion;

    }

    public static void createAccount(String accountId, String jwt, String email) {
        try (Connection conn = getDatabaseConnection()) {


            PreparedStatement ps = conn.prepareStatement("INSERT INTO `accounts` (`id`, `account`, `jwt`, `created`, `deactivated`, `email`) VALUES (NULL, ?, ?, ?, ?, ?) ");

            ps.setString(1, accountId);
            ps.setString(2, jwt);
            ps.setTimestamp(3, dateToTimestamp(new Date())); //Current Date and Time
            ps.setBoolean(4, false);
            ps.setString(5, email);

            ps.execute();
            log.info("New ACME account created with account id \"" + accountId + "\"");

        } catch (SQLException e) {
            log.error("Unable to create new ACME account", e);
        }


    }

    public static void passChallenge(String challengeId){
        try (Connection conn = getDatabaseConnection()) {


            PreparedStatement ps = conn.prepareStatement("UPDATE `orderidentifiers` SET `verified` = ?, `verifiedtime` = ? WHERE `orderidentifiers`.`challengeid` = ? ");

            ps.setBoolean(1, true);
            ps.setTimestamp(2, dateToTimestamp(new Date())); //Current Date and Time
            ps.setString(3, challengeId);

            ps.execute();
            log.info("ACME challenge with id \"" + challengeId + "\" was marked as passed");

        } catch (SQLException e) {
            log.error("Unable to create new ACME account", e);
        }
    }

    public static ACMEIdentifier getACMEIdentifierByChallengeId(String authorizationId) {
        ACMEIdentifier identifier = null;

        try (Connection conn = getDatabaseConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM `orderidentifiers` WHERE challengeid = ? LIMIT 1");
            ps.setString(1,authorizationId);


            // process the results
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                String type = rs.getString("type");
                String value = rs.getString("value");
                String authorizationToken = rs.getString("authorizationtoken");
                String challengeId = rs.getString("challengeid");
                // String challengeType = rs.getString("challengetype");

                Boolean verified = rs.getBoolean("verified");
                Date validationDate = rs.getTimestamp("verifiedtime");


                log.info("(Challenge ID: \"" + authorizationId + "\") Got ACME identifier of type \"" + type + "\" with value \"" + value + "\"");

                identifier = new ACMEIdentifier(type, value, authorizationId, authorizationToken, challengeId, verified, validationDate);
            }

        } catch (SQLException e) {
            log.error("Unable get ACME identifiers for challenge id \"" + authorizationId + "\"",e);
        }

        return identifier;
    }
    public static ACMEIdentifier getACMEIdentifierByAuthorizationId(String authorizationId) {

        ACMEIdentifier identifier = null;

        try (Connection conn = getDatabaseConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM `orderidentifiers` WHERE authorizationid = ? LIMIT 1");
            ps.setString(1,authorizationId);


            // process the results
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                String type = rs.getString("type");
                String value = rs.getString("value");
                String authorizationToken = rs.getString("authorizationtoken");
                String challengeId = rs.getString("challengeid");
                Boolean verified = rs.getBoolean("verified");
                Date validationDate = rs.getTimestamp("verifiedtime");
                // String challengeType = rs.getString("challengetype");

                log.info("(Authorization ID: \"" + authorizationId + "\") Got ACME identifier of type \"" + type + "\" with value \"" + value + "\"");

                identifier = new ACMEIdentifier(type, value, authorizationId, authorizationToken, challengeId, verified, validationDate);
            }

        } catch (SQLException e) {
            log.error("Unable get ACME identifiers for autorization id \"" + authorizationId + "\"",e);
        }

        return identifier;
    }

    public static void createOrder(String accountId, String orderId, List<ACMEIdentifier> identifierList) {


        try (Connection conn = getDatabaseConnection()) {


            PreparedStatement psOrder = conn.prepareStatement("INSERT INTO `orders` (`id`, `orderid`, `account`, `created`) VALUES (NULL, ?, ?, ?) ");
            psOrder.setString(1, orderId);
            psOrder.setString(2, accountId);
            psOrder.setTimestamp(3, dateToTimestamp(new Date())); //Current Date and Time
            psOrder.execute();

            log.info("Created new order \"" + orderId + "\"");

            for (ACMEIdentifier identifier : identifierList) {

                if (identifier.getAuthorizationId() == null || identifier.getAuthorizationToken() == null) {
                    throw new RuntimeException("Authorization Id or Token is null");
                }

                PreparedStatement psOrderIdentifiers = conn.prepareStatement("INSERT INTO `orderidentifiers` (`id`, `orderid`, `type`, `value`, `verified`, `verifiedtime`, `authorizationId`, `authorizationtoken`, `challengeid`) VALUES (NULL, ?, ?, ?, ?, NULL, ?, ?, ?) ");
                psOrderIdentifiers.setString(1, orderId);
                psOrderIdentifiers.setString(2, identifier.getType());
                psOrderIdentifiers.setString(3, identifier.getValue());
                psOrderIdentifiers.setBoolean(4, false);
                psOrderIdentifiers.setString(5, identifier.getAuthorizationId());
                psOrderIdentifiers.setString(6, identifier.getAuthorizationToken());
                psOrderIdentifiers.setString(7, identifier.getChallengeId());
                psOrderIdentifiers.execute();
                log.info("Added identifier \"" + identifier.getValue() + "\" of type \"" + identifier.getType() + "\" to order \"" + orderId + "\"");


            }


        } catch (SQLException e) {
            log.error("Unable to create new ACME Order with id \"" + orderId + "\" for account \"" + accountId + "\"", e);
        }


    }

    public static void updateAccountEmail(String accountId, String email) {
        try (Connection conn = getDatabaseConnection()) {


            PreparedStatement ps = conn.prepareStatement("UPDATE `accounts` SET `email` = ? WHERE `accounts`.`account` = ? ");

            ps.setString(1, email);
            ps.setString(2, accountId);

            ps.execute();
            log.info("ACME account \"" + accountId + "\" updated email to \"" + email + "\"");

        } catch (SQLException e) {
            log.error("Unable to update email for account \"" + accountId + "\"", e);
        }


    }

    private static Timestamp dateToTimestamp(Date date) {
        return new Timestamp(date.getTime());
    }


}
