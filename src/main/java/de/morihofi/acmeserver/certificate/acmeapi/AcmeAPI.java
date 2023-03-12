package de.morihofi.acmeserver.certificate.acmeapi;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.certificate.Database;
import de.morihofi.acmeserver.certificate.objects.ACMEIdentifier;
import de.morihofi.acmeserver.certificate.tools.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Route;
import spark.Spark;

import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class AcmeAPI {
    public static final Logger log = LogManager.getLogger(Database.class);

    public static Route newNonce = (request, response) -> {


        if (request.requestMethod().equals("GET")) {
            response.status(204);
        }
        if (request.requestMethod().equals("HEAD")) {
            response.status(200);
        }

        response.header("Cache-Control", "no-store");
        response.header("Link", "<" + getApiURL() + "/directory" + ">;rel=\"index\"");
        response.header("Replay-Nonce", Crypto.createNonce());

        return "";
    };

    /**
     * Account Update Endpoint
     * <p>
     * URL: /acme/acct/1 (1 <- Is the id in this sample)
     */
    public static Route account = (request, response) -> {
        String accountId = request.params("id");

        JSONObject reqBodyObj = new JSONObject(request.body());

        //Payload is Base64 Encoded
        JSONObject reqBodyPayloadObj = new JSONObject(Base64Tools.decodeBase64(reqBodyObj.getString("payload")));
        JSONObject reqBodyProtectedObj = new JSONObject(Base64Tools.decodeBase64(reqBodyObj.getString("protected")));

        //TODO: Check signature


        // Update Account Settings, e.g. E-Mail change

        if (reqBodyPayloadObj.has("contact")) {
            String contactEmail = reqBodyPayloadObj.getJSONArray("contact").getString(0);
            contactEmail = contactEmail.replace("mailto:", "");



            if (!RegexTools.isValidEmail(contactEmail) || contactEmail.split("\\@")[1].equals("localhost")){
                log.error("E-Mail validation failed for email \"" + contactEmail + "\n");
                response.header("Content-Type", "application/problem+json");
                JSONObject resObj = new JSONObject();
                resObj.put("type", "urn:ietf:params:acme:error:invalidContact");
                resObj.put("detail", "E-Mail address is invalid");
                Spark.halt(HttpURLConnection.HTTP_FORBIDDEN, resObj.toString());
            }
            log.info("E-Mail validation successful for email \"" + contactEmail + "\n");

            //Update Contact Email
            Database.updateAccountEmail(accountId, contactEmail);
        }


        JSONObject responseJSON = new JSONObject();
        response.header("Content-Type", "application/jose+json");
        response.status(200);

        return responseJSON.toString();
    };

    /**
     * New Order Endpoint
     * <p>
     * URL: /acme/new-order
     */
    public static Route newOrder = (request, response) -> {


        //Parse request body
        JSONObject reqBodyObj = new JSONObject(request.body());
        JSONObject reqBodyPayloadObj = new JSONObject(Base64Tools.decodeBase64(reqBodyObj.getString("payload")));
        JSONObject reqBodyProtectedObj = new JSONObject(Base64Tools.decodeBase64(reqBodyObj.getString("protected")));


        //TODO: Validate Client Signature


        JSONArray identifiersArr = reqBodyPayloadObj.getJSONArray("identifiers");

        ArrayList<ACMEIdentifier> acmeIdentifiers = new ArrayList<>();

        //Currently only one DNS entry is supported
        if (identifiersArr.length() != 1){
            //Throw unsupported error
            log.error("Too many domains requested. Only one per order is currently supported");
            response.header("Content-Type", "application/problem+json");
            JSONObject resObj = new JSONObject();
            resObj.put("type", "urn:ietf:params:acme:error:malformed");
            resObj.put("detail", "Too many domains requested. Only one per order is currently supported");
            Spark.halt(HttpURLConnection.HTTP_BAD_REQUEST, resObj.toString());
        }


        for (int i = 0; i < identifiersArr.length(); i++) {
            JSONObject identifier = identifiersArr.getJSONObject(i);

            String type = identifier.getString("type");
            String value = identifier.getString("value");


            acmeIdentifiers.add(new ACMEIdentifier(type, value));
        }


        JSONObject returnObj = new JSONObject();

        // Create order in Database
        String orderId = UUID.randomUUID().toString();
        String accountId = getAccountIdFromProtected(reqBodyProtectedObj);


        JSONArray respIdentifiersArr = new JSONArray();
        JSONArray respAuthorizationsArr = new JSONArray();

        ArrayList<ACMEIdentifier> acmeIdentifiersWithAuthorizationData = new ArrayList<>();

        for (ACMEIdentifier identifier : acmeIdentifiers) {
            JSONObject identifierObj = new JSONObject();
            identifierObj.put("type", identifier.getType());
            identifierObj.put("value", identifier.getValue());
            respIdentifiersArr.put(identifierObj);

            // Unique value for each domain
            String authorizationId = Crypto.hashStringSHA256(identifier.getType() + "." + identifier.getType() + "." + DateTools.formatDateForACME(new Date()));
            // Random authorization token
            String authorizationToken = Crypto.hashStringSHA256(identifier.getType() + "." + identifier.getType() + "." + System.nanoTime() + "--token");
            // Unique challenge id
            String challengeId = Crypto.hashStringSHA256(identifier.getType() + "." + identifier.getType() + "." + (System.nanoTime() / 100 * 1.557) + "--challenge");
            // Unique certificate id
            String certificateId = Crypto.hashStringSHA256(identifier.getType() + "." + identifier.getType() + "." + (System.nanoTime() / 100 * 5.579) + "--cert");


            identifier.setAuthorizationToken(authorizationToken);
            identifier.setAuthorizationId(authorizationId);
            identifier.setChallengeId(challengeId);
            identifier.setCertificateId(certificateId);

            acmeIdentifiersWithAuthorizationData.add(identifier);

            respAuthorizationsArr.put(getApiURL() + "/acme/authz/" + authorizationId);


        }
        // Add authorizations to Database
        Database.createOrder(accountId, orderId, acmeIdentifiersWithAuthorizationData);

        //TODO: Set better Date/Time

        returnObj.put("status", "pending");
        returnObj.put("expires", DateTools.formatDateForACME(new Date()));
        returnObj.put("notBefore", DateTools.formatDateForACME(new Date()));
        returnObj.put("notAfter", DateTools.formatDateForACME(new Date()));
        returnObj.put("identifiers", respIdentifiersArr);
        returnObj.put("authorizations", respAuthorizationsArr);
        returnObj.put("finalize", getApiURL() + "/acme/order/" + orderId + "/finalize");

        response.status(201);
        response.header("Link", "<" + getApiURL() + "/directory" + ">;rel=\"index\"");
        response.header("Replay-Nonce", Crypto.createNonce());
        response.header("Content-Type", "application/jose+json");
        response.header("Location", getApiURL() + "/acme/order/" + orderId);

        return returnObj.toString();

    };
    /**
     * Ownership verification endpoint
     * <p>
     * URL: /acme/authz/5429d8d61a406 ...
     */
    public static Route authz = (request, response) -> {
        String authorizationId = request.params("authorizationId");

        response.header("Content-Type", "application/jose+json");
        response.header("Replay-Nonce", Crypto.createNonce());
        response.status(200);

        //TODO: Not found response if identifier is null

        ACMEIdentifier identifier = Database.getACMEIdentifierByAuthorizationId(authorizationId);

        //Not found
        if(identifier == null){
            log.error("Throwing API error: Host verification failed");
            response.header("Content-Type", "application/problem+json");
            JSONObject resObj = new JSONObject();
            resObj.put("type", "urn:ietf:params:acme:error:malformed");
            resObj.put("detail", "The requested authorization id was not found");
            Spark.halt(HttpURLConnection.HTTP_NOT_FOUND, resObj.toString());
        }

        JSONObject identifierObj = new JSONObject();
        identifierObj.put("type", identifier.getType());
        identifierObj.put("value", identifier.getValue());


        JSONArray challengeArr = new JSONArray();

        JSONObject challengeHTTP = new JSONObject();
        challengeHTTP.put("type", "http-01"); //
        challengeHTTP.put("url", getApiURL() + "/acme/chall/" + identifier.getChallengeId()); //Challenge callback URL, called after created token on server
        challengeHTTP.put("token", identifier.getAuthorizationToken());
        if (identifier.isVerified()) {
            challengeHTTP.put("status", "valid");
            challengeHTTP.put("validated", DateTools.formatDateForACME(identifier.getVerifiedDate()));
        }
        challengeArr.put(challengeHTTP);


        JSONObject returnObj = new JSONObject();

        if (identifier.isVerified()) {
            returnObj.put("status", "valid");
        } else {
            returnObj.put("status", "pending");
        }
        returnObj.put("expires", DateTools.formatDateForACME(new Date()));
        returnObj.put("identifier", identifierObj);
        returnObj.put("challenges", challengeArr);


        return returnObj.toString();
    };

    /**
     * Verify ACME Challenge Callback (I've placed it on the server, can you check it now?)
     * <p>
     * This is called once
     * <p>
     * URL: /acme/chall/ktjlr ...
     */
    public static Route challengeCallback = (request, response) -> {
        String challengeId = request.params("challengeId");

        response.header("Content-Type", "application/json");
        response.header("Replay-Nonce", Crypto.createNonce());


        //heck if challenge is valid
        ACMEIdentifier identifier = Database.getACMEIdentifierByChallengeId(challengeId);
        log.info("Validating ownership of host \"" + identifier.getValue() + "\"");
        if(HTTPChallenge.check(challengeId, identifier.getAuthorizationToken(), identifier.getValue())){
            //mark challenge has passed
            Database.passChallenge(challengeId);
        }else {
            log.error("Throwing API error: Host verification failed");
            response.header("Content-Type", "application/problem+json");
            JSONObject resObj = new JSONObject();
            resObj.put("type", "urn:ietf:params:acme:error:connection");
            resObj.put("detail", "Unable to reach host or invalid token. Is the host reachable? Is the http server on port 80 running? If it is running, check your access logs");
            Spark.halt(HttpURLConnection.HTTP_FORBIDDEN, resObj.toString());


            // TODO: Fail challenge in database
            // Database.failChallenge(challengeId);


        }


        //Reload identifier, e.g. host has validated
        identifier = Database.getACMEIdentifierByChallengeId(challengeId);


        JSONObject responseJSON = new JSONObject();
        responseJSON.put("type", "http-01");
        if (identifier.isVerified()) {
            responseJSON.put("status", "valid");
            responseJSON.put("verified", DateTools.formatDateForACME(identifier.getVerifiedDate()));
        } else {
            responseJSON.put("status", "pending");
        }


        //"Up"-Link header is required for certbot
        response.header("Link", "<" + getApiURL() + "/acme/authz/" + identifier.getAuthorizationId() + ">;rel=\"up\"");
        responseJSON.put("url", getApiURL() + "/acme/chall/" + challengeId);
        responseJSON.put("token", identifier.getAuthorizationToken());


        return responseJSON;

    };

    /**
     * Sign CSR (Finalizing ACME)
     * <p>
     * URL: /acme/order/thisisanorderid/finalize
     */
    public static Route finalizeOrder = (request, response) -> {
        String orderId = request.params("orderid");

        JSONObject reqBodyObj = new JSONObject(request.body());
        JSONObject reqBodyPayloadObj = new JSONObject(Base64Tools.decodeBase64(reqBodyObj.getString("payload")));

        JSONObject responseJSON = new JSONObject();

        String csr = reqBodyPayloadObj.getString("csr");

        //TODO: Check signature
        //TODO: Check if CSR Domain-names are matching with requested and checked Domain-names

        ACMEIdentifier identifier = Database.getACMEIdentifierByOrderId(orderId);

        JSONObject identifiersObj = new JSONObject();
        identifiersObj.put("type", identifier.getType());
        identifiersObj.put("value", identifier.getValue());

        JSONArray authorizationsArr = new JSONArray();
        authorizationsArr.put(getApiURL() + "/acme/authz/" + identifier.getAuthorizationId());

        try {


                byte[] csrBytes = CertTools.decodeBase64URLAsBytes(csr);
                PKCS10CertificationRequest csrObj = new PKCS10CertificationRequest(csrBytes);
                PemObject pkPemObject = new PemObject("PUBLIC KEY", csrObj.getSubjectPublicKeyInfo().getEncoded());
                //RSAKeyParameters pubKey = (RSAKeyParameters) PublicKeyFactory.createKey(csrObj.getSubjectPublicKeyInfo());

                log.info("Creating Certificate for order \"" + orderId + "\" with DNS Name \"" + identifier.getValue() + "\"");
                X509Certificate acmeGeneratedCertificate = CertTools.createServerCertificate(Main.intermediateKeyPair, Main.intermediateCertificate.getEncoded(), pkPemObject.getContent(), new String[]{identifier.getValue()}, Main.acmeCertificatesExpireDays, Main.acmeCertificatesExpireMonths, Main.acmeCertificatesExpireYears);

                String pemCertificate = CertTools.certificateToPEM(acmeGeneratedCertificate.getEncoded());

                Date expireDate = acmeGeneratedCertificate.getNotAfter();
                Date issueDate = acmeGeneratedCertificate.getNotBefore();


                Database.storeCertificateInDatabase(orderId, identifier.getValue(), csr, pemCertificate, issueDate, expireDate);


                response.header("Content-Type", "application/json");
                response.header("Replay-Nonce", Crypto.createNonce());
                response.header("Location", getApiURL() + "/acme/order/" + orderId);



                responseJSON.put("status", "valid");
                responseJSON.put("expires", DateTools.formatDateForACME(expireDate));
                responseJSON.put("finalize", getApiURL() + "/acme/order/" + orderId + "/finalize");
                responseJSON.put("certificate", getApiURL() + "/acme/order/" + orderId + "/cert"); //Temporary fix
                //responseJSON.put("certificate", getApiURL() + "/acme/cert/" + identifier.getCertificateId());
                responseJSON.put("authorizations", authorizationsArr);


                return responseJSON.toString();

        }catch (Exception ex){

            log.error("Throwing API error: CSR processing error",ex);
            response.header("Content-Type", "application/problem+json");
            JSONObject resObj = new JSONObject();
            resObj.put("type", "urn:ietf:params:acme:error:badCSR");
            resObj.put("detail", "Unable process requested CSR. Is the CSR valid? Otherwise try again later, if the problem persists contact support.");
            Spark.halt(HttpURLConnection.HTTP_FORBIDDEN, resObj.toString());

            //It halts before, so this is never executed
            return "";
        }

    };

    /**
     * Order info Endpoint
     * <p>
     * URL: /order/orderid123
     */
    public static Route order = (request, response) -> {

        String orderId = request.params("orderid");

        response.header("Content-Type", "application/json");
        response.header("Replay-Nonce", Crypto.createNonce());

        JSONObject reqBodyObj = new JSONObject(request.body());
        //Payload is Base64 Encoded
        // Payload is empty here
        //JSONObject reqBodyPayloadObj = new JSONObject(Base64Tools.decodeBase64(reqBodyObj.getString("payload")));
        JSONObject reqBodyProtectedObj = new JSONObject(Base64Tools.decodeBase64(reqBodyObj.getString("protected")));

        JSONObject responseObj = new JSONObject();



        // TODO: Valid Expires Date
        responseObj.put("expires", DateTools.formatDateForACME(new Date()));



        JSONArray identifiersArr = new JSONArray();
        JSONArray authorizationsArr = new JSONArray();

        boolean allVerified = true;
        for (ACMEIdentifier identifier: Database.getACMEIdentifiersByOrderId(orderId)) {

            if(!identifier.isVerified()){
                allVerified = false;
            }

            JSONObject identifierObj = new JSONObject();
            identifierObj.put("type",identifier.getType());
            identifierObj.put("value",identifier.getValue());

            authorizationsArr.put(getApiURL() + "/acme/authz/" + identifier.getAuthorizationId());

        }


        responseObj.put("identifiers", identifiersArr);
        if (allVerified) {
            responseObj.put("status", "valid");
        } else {
            responseObj.put("status", "pending");
        }


        responseObj.put("finalize", getApiURL() + "/acme/order/" + orderId + "/finalize");
        // Get certificate for order
        //responseObj.put("certificate", getApiURL() + "/acme/cert/" + "fixme");
        responseObj.put("certificate", getApiURL() + "/acme/order/" + orderId + "/cert");


        return responseObj.toString();
    };

    /**
     * Get certificate of order (last step in certbot)
     * URL: /order/orderid123/cert
     */
    public static Route orderCert = (request, response) -> {
        String orderId = request.params("orderid");

        response.header("Content-Type", "application/pem-certificate-chain");
        response.header("Replay-Nonce", Crypto.createNonce());
        response.header("Link", "<" + getApiURL() + "/directory" + ">;rel=\"index\"");

        ACMEIdentifier identifier = Database.getACMEIdentifierByOrderId(orderId);

        String responseCertificateChain = Database.getCertificateChainPEMofACMEbyAuthorizationId(identifier.getAuthorizationId());

        return responseCertificateChain;

    };
    public static Route downloadCA = (request, response) -> {
        response.header("Content-Type", "application/x-x509-ca-cert");

        return new String(Files.readAllBytes(Main.caPath));
    };


    /**
     * Get the ACME Server URL, reachable from other Hosts
     *
     * @return Full url (including HTTPS prefix) and port to this server
     */
    public static String getApiURL() {
        return "https://" + Main.acmeThisServerDNSName + ":" + Main.acmeThisServerAPIPort;
    }


    private static String getAccountIdFromProtected(JSONObject protectedObj) {

        String kid = protectedObj.getString("kid");

        //Cut the url, starts from account id
        return kid.substring((getApiURL() + "/acme/acct/").length());
    }

    /**
     * Get ACME Directory Info Endpoint
     * <p>
     * URL: /directory
     */
    public static Route directoryEndpoint = (request, response) -> {
        // Response is JSON
        response.header("Content-Type", "application/json");

        JSONObject responseJSON = new JSONObject();

        JSONObject metaObject = new JSONObject();
        metaObject.put("website", Main.acmeMetaWebsite);
        metaObject.put("termsOfService", Main.acmeMetaTermsOfService);


        responseJSON.put("meta", metaObject);
        responseJSON.put("newAccount", getApiURL() + "/acme/new-acct");
        responseJSON.put("newNonce", getApiURL() + "/acme/new-nonce");
        responseJSON.put("newOrder", getApiURL() + "/acme/new-order");
        responseJSON.put("revokeCert", getApiURL() + "/acme/revoke-cert");
        responseJSON.put("keyChange", getApiURL() + "/acme/key-change");

        return responseJSON.toString();
    };



    /**
     * Create a new ACME Account Endpoint
     * <p>
     * URL: /acme/new-acct
     */
    public static Route newAccount = (request, response) -> {

        JSONObject reqBodyObj = new JSONObject(request.body());

        //Payload is Base64 Encoded
        JSONObject reqBodyPayloadObj = new JSONObject(Base64Tools.decodeBase64(reqBodyObj.getString("payload")));
        JSONObject reqBodyProtectedObj = new JSONObject(Base64Tools.decodeBase64(reqBodyObj.getString("protected")));

        boolean reqPayloadTermsOfServiceAgreed = reqBodyPayloadObj.getBoolean("termsOfServiceAgreed");
        String reqPayloadContactEmail = "";
        // Has email? (This can be updated later)
        if (reqBodyPayloadObj.has("contact")) {
            reqPayloadContactEmail = reqBodyPayloadObj.getJSONArray("contact").getString(0);
            reqPayloadContactEmail = reqPayloadContactEmail.replace("mailto:", "");

            if (!RegexTools.isValidEmail(reqPayloadContactEmail) || reqPayloadContactEmail.split("\\@")[0].equals("localhost")){
                log.error("E-Mail validation failed for email \"" + reqPayloadContactEmail + "\n");
                response.header("Content-Type", "application/problem+json");
                JSONObject resObj = new JSONObject();
                resObj.put("type", "urn:ietf:params:acme:error:invalidContact");
                resObj.put("detail", "E-Mail address is invalid");
                Spark.halt(HttpURLConnection.HTTP_FORBIDDEN, resObj.toString());
            }
            log.info("E-Mail validation successful for email \"" + reqPayloadContactEmail + "\n");

        }


        // Create new account in database
        // https://ietf-wg-acme.github.io/acme/draft-ietf-acme-acme.html#rfc.section.7.3

        String accountId = UUID.randomUUID().toString();
        Database.createAccount(accountId, reqBodyProtectedObj.getJSONObject("jwk").toString(), reqPayloadContactEmail);

        String nonce = Crypto.createNonce();
        // Response is JSON
        response.header("Content-Type", "application/json");
        response.header("Location", getApiURL() + "/acme/acct/" + accountId);
        response.header("Replay-Nonce", nonce);
        response.status(201); //Created

        JSONObject responseJSON = new JSONObject();


        //Contact information
        JSONArray contactEmailsArr = new JSONArray();
        contactEmailsArr.put(reqPayloadContactEmail);

        responseJSON.put("status", "valid");
        if (contactEmailsArr.length() != 0) {
            responseJSON.put("contact", contactEmailsArr);
        }


        responseJSON.put("orders", getApiURL() + "/acme/acct/" + accountId + "/orders");


        return responseJSON.toString();
    };

}
