package de.morihofi.acmeserver.certificate;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.certificate.acmeapi.AcmeAPI;
import de.morihofi.acmeserver.certificate.objects.ACMEIdentifier;
import de.morihofi.acmeserver.certificate.tools.CertTools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.units.qual.A;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.CertificateEncodingException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class Database {

    public static Logger log = LogManager.getLogger(Database.class);

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

    /**
     * This function marks a ACME challenge as passed
     * @param challengeId Id of the Challenge, provided in URL
     */
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

    public static ACMEIdentifier getACMEIdentifierByChallengeId(String challengeId) {
        ACMEIdentifier identifier = null;

        try (Connection conn = getDatabaseConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM `orderidentifiers` WHERE challengeid = ? LIMIT 1");
            ps.setString(1,challengeId);


            // process the results
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                String type = rs.getString("type");
                String value = rs.getString("value");
                String authorizationToken = rs.getString("authorizationtoken");
                // String challengeType = rs.getString("challengetype");
                String authorizationId = rs.getString("authorizationid");
                Boolean verified = rs.getBoolean("verified");
                Date validationDate = rs.getTimestamp("verifiedtime");

                String certificateId = rs.getString("certificateid");
                String certificateCSR = rs.getString("certificatecsr");
                Date certificateIssued = rs.getTimestamp("certificateissued");
                Date certificateExpires = rs.getTimestamp("certificateexpires");


                log.info("(Challenge ID: \"" + challengeId + "\") Got ACME identifier of type \"" + type + "\" with value \"" + value + "\"");

                identifier = new ACMEIdentifier(type, value, authorizationId, authorizationToken, challengeId, verified, validationDate, certificateId, certificateCSR,certificateIssued,certificateExpires);

            }

        } catch (SQLException e) {
            log.error("Unable get ACME identifiers for challenge id \"" + challengeId + "\"",e);
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

                String certificateId = rs.getString("certificateid");
                String certificateCSR = rs.getString("certificatecsr");
                Date certificateIssued = rs.getTimestamp("certificateissued");
                Date certificateExpires = rs.getTimestamp("certificateexpires");

                log.info("(Authorization ID: \"" + authorizationId + "\") Got ACME identifier of type \"" + type + "\" with value \"" + value + "\"");

                identifier = new ACMEIdentifier(type, value, authorizationId, authorizationToken, challengeId, verified, validationDate, certificateId, certificateCSR,certificateIssued,certificateExpires);
            }

        } catch (SQLException e) {
            log.error("Unable get ACME identifiers for autorization id \"" + authorizationId + "\"",e);
        }

        return identifier;
    }


    public static List<ACMEIdentifier> getACMEIdentifiersByOrderId(String orderId) {

        ArrayList<ACMEIdentifier> identifiers = new ArrayList<>();

        try (Connection conn = getDatabaseConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM `orderidentifiers` WHERE orderid = ? LIMIT 1");
            ps.setString(1,orderId);


            // process the results
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                String type = rs.getString("type");
                String value = rs.getString("value");
                String authorizationToken = rs.getString("authorizationtoken");
                String authorizationId = rs.getString("authorizationid");
                String challengeId = rs.getString("challengeid");
                Boolean verified = rs.getBoolean("verified");
                Date validationDate = rs.getTimestamp("verifiedtime");
                // String challengeType = rs.getString("challengetype");

                String certificateId = rs.getString("certificateid");
                String certificateCSR = rs.getString("certificatecsr");
                Date certificateIssued = rs.getTimestamp("certificateissued");
                Date certificateExpires = rs.getTimestamp("certificateexpires");


                log.info("(Order ID: \"" + orderId + "\") Got ACME identifier of type \"" + type + "\" with value \"" + value + "\"");

                identifiers.add(new ACMEIdentifier(type, value, authorizationId, authorizationToken, challengeId, verified, validationDate, certificateId, certificateCSR,certificateIssued,certificateExpires));
            }

        } catch (SQLException e) {
            log.error("Unable get ACME identifiers for order id \"" + orderId + "\"",e);
        }

        return identifiers;

    }

    public static ACMEIdentifier getACMEIdentifierByOrderId(String orderId) {
        //FIXME
        return getACMEIdentifiersByOrderId(orderId).get(0);
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

                PreparedStatement psOrderIdentifiers = conn.prepareStatement("INSERT INTO `orderidentifiers` (`id`, `orderid`, `type`, `value`, `verified`, `verifiedtime`, `authorizationId`, `authorizationtoken`, `challengeid`,`certificateid`) VALUES (NULL, ?, ?, ?, ?, NULL, ?, ?, ?, ?) ");
                psOrderIdentifiers.setString(1, orderId);
                psOrderIdentifiers.setString(2, identifier.getType());
                psOrderIdentifiers.setString(3, identifier.getValue());
                psOrderIdentifiers.setBoolean(4, false);
                psOrderIdentifiers.setString(5, identifier.getAuthorizationId());
                psOrderIdentifiers.setString(6, identifier.getAuthorizationToken());
                psOrderIdentifiers.setString(7, identifier.getChallengeId());
                psOrderIdentifiers.setString(8, identifier.getCertificateId());
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


    public static void storeCertificateInDatabase(String orderId,String dnsValue, String csr, String pemCertificate, Date issueDate, Date expireDate) {

        //TODO: Check if orderId exists


        try (Connection conn = getDatabaseConnection()) {


            PreparedStatement ps = conn.prepareStatement("UPDATE `orderidentifiers` SET `certificatecsr` = ?, `certificateissued` = ?, `certificateexpires` = ?, `certificatepem` = ? WHERE `orderidentifiers`.`orderid` = ? AND `orderidentifiers`.`value` = ?");

            ps.setString(1, csr);
            ps.setTimestamp(2, dateToTimestamp(issueDate));
            ps.setTimestamp(3, dateToTimestamp(expireDate));
            ps.setString(4,pemCertificate);
            ps.setString(5,orderId);
            ps.setString(6,dnsValue);

            ps.execute();
            log.info("Stored certificate successful");

        } catch (SQLException e) {
            log.error("Unable to store certificate", e);
        }


    }

    public static String getCertificateChainPEMofACMEbyAuthorizationId(String authorizationId) throws CertificateEncodingException, IOException {
        StringBuilder pemBuilder = new StringBuilder();
        boolean certFound = false;
        // Get Issued certificate
        try (Connection conn = getDatabaseConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM `orderidentifiers` WHERE authorizationId = ? LIMIT 1");
            ps.setString(1, authorizationId);


            // process the results
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                certFound = true;

                String certificateId = rs.getString("certificateid");
                String certificateCSR = rs.getString("certificatecsr");
                Date certificateIssued = rs.getTimestamp("certificateissued");
                Date certificateExpires = rs.getTimestamp("certificateexpires");
                String certificatePEM = rs.getString("certificatepem");


                log.info("Getting Certificate for authorization Id \"" + authorizationId + "\"");

                pemBuilder.append(certificatePEM);

            }

        } catch (SQLException e) {
            log.error("Unable get Certificate for authorization id \"" + authorizationId + "\"",e);
        }

        if(!certFound){
            throw new IllegalArgumentException("No certificate was found for authorization id \"" + authorizationId + "\"");
        }

        //Intermediate Certificate
        log.info("Adding Intermediate certificate");
        pemBuilder.append(CertTools.certificateToPEM(Main.intermediateCertificate.getEncoded()) + "\n");

        //CA Certificate
        log.info("Adding CA certificate");
        try (Stream<String> stream = Files.lines(Main.caPath, StandardCharsets.UTF_8)) {
            stream.forEach(s -> pemBuilder.append(s).append("\n"));
        } catch (IOException e) {
            //handle exception
            log.error("Unable to load CA Certificate", e);
        }



        System.out.println(pemBuilder.toString());

        return pemBuilder.toString();
    }


}
