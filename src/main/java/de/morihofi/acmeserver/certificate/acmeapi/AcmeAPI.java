package de.morihofi.acmeserver.certificate.acmeapi;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.certificate.Database;
import de.morihofi.acmeserver.certificate.objects.ACMEIdentifier;
import de.morihofi.acmeserver.certificate.tools.Base64Tools;
import de.morihofi.acmeserver.certificate.tools.CertTools;
import de.morihofi.acmeserver.certificate.tools.Crypto;
import de.morihofi.acmeserver.certificate.tools.DateTools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Route;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class AcmeAPI {
    public static Logger log = LogManager.getLogger(Database.class);

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
     * Verify ACME Challenge Callback
     * <p>
     * URL: /acme/chall/ktjlr ...
     */
    public static Route challengeCallback = (request, response) -> {
        String challengeId = request.params("challengeId");

        response.header("Content-Type", "application/json");
        response.header("Replay-Nonce", Crypto.createNonce());


        //TODO: Check if challenge is valid

        //mark challenge has passed
        Database.passChallenge(challengeId);

        ACMEIdentifier identifier = Database.getACMEIdentifierByChallengeId(challengeId);


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

        String csr = reqBodyPayloadObj.getString("csr");

        //TODO: Check signature
        //TODO: Check if CSR Domain-names are matching with requested and checked Domain-names

        ACMEIdentifier identifier = Database.getACMEIdentifierByOrderId(orderId);

        JSONObject identifiersObj = new JSONObject();
        identifiersObj.put("type", identifier.getType());
        identifiersObj.put("value", identifier.getValue());

        JSONArray authorizationsArr = new JSONArray();
        authorizationsArr.put(getApiURL() + "/acme/authz/" + identifier.getAuthorizationId());


        byte[] csrBytes = CertTools.decodeBase64URLAsBytes(csr);
        PKCS10CertificationRequest csrObj = new PKCS10CertificationRequest(csrBytes);
        PemObject pkPemObject = new PemObject("PUBLIC KEY", csrObj.getSubjectPublicKeyInfo().getEncoded());
        //RSAKeyParameters pubKey = (RSAKeyParameters) PublicKeyFactory.createKey(csrObj.getSubjectPublicKeyInfo());

        log.info("Creating Certificate for order \"" + orderId + "\" with DNS Name \"" + identifier.getValue() + "\"");
        X509Certificate acmeGeneratedCertificate = CertTools.createServerCertificate(Main.intermediateKeyPair, Main.intermediateCertificate.getEncoded(), pkPemObject.getContent(), new String[]{identifier.getValue()}, 14, 0, 0);

        String pemCertificate = CertTools.certificateToPEM(acmeGeneratedCertificate.getEncoded());

        Date expireDate = acmeGeneratedCertificate.getNotAfter();
        Date issueDate = acmeGeneratedCertificate.getNotBefore();


        Database.storeCertificateInDatabase(orderId, identifier.getValue(),csr, pemCertificate, issueDate, expireDate);


        response.header("Content-Type", "application/json");
        response.header("Replay-Nonce", Crypto.createNonce());
        response.header("Location", getApiURL() + "/acme/order/" + orderId);

        JSONObject responseJSON = new JSONObject();

        responseJSON.put("status", "valid");
        responseJSON.put("expires", DateTools.formatDateForACME(expireDate));
        responseJSON.put("finalize", getApiURL() + "/acme/order/" + orderId + "/finalize");
        responseJSON.put("certificate", getApiURL() + "/acme/cert/" + identifier.getCertificateId());
        responseJSON.put("authorizations", authorizationsArr);

        System.out.println(responseJSON.toString(4));

        return responseJSON.toString();
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
