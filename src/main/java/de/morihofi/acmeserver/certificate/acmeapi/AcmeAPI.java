package de.morihofi.acmeserver.certificate.acmeapi;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.certificate.objects.ACMEIdentifier;
import de.morihofi.acmeserver.certificate.tools.Base64Tools;
import de.morihofi.acmeserver.certificate.tools.Crypto;
import de.morihofi.acmeserver.certificate.tools.DateTools;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Route;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class AcmeAPI {


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


        JSONObject responseJSON = new JSONObject();

        // TODO: Update Account Settings, e.g. E-Mail change


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

        response.status(201);

        //Parse request body
        JSONObject reqBodyObj = new JSONObject(request.body());

        //TODO: Validate Client Signature



        JSONObject reqBodyPayloadObj = new JSONObject(Base64Tools.decodeBase64(reqBodyObj.getString("payload")));

        JSONArray identifiersArr = reqBodyPayloadObj.getJSONArray("identifiers");

        ArrayList<ACMEIdentifier> acmeIdentifiers = new ArrayList<>();

        for (int i = 0; i < identifiersArr.length(); i++) {
            JSONObject identifier = identifiersArr.getJSONObject(i);

            String type = identifier.getString("type");
            String value = identifier.getString("value");


            acmeIdentifiers.add(new ACMEIdentifier(type, value));
        }



        JSONObject returnObj = new JSONObject();

        // TODO: Create order in Database
        String orderId = UUID.randomUUID().toString();

        JSONArray respIdentifiersArr = new JSONArray();
        JSONArray respAuthorizationsArr = new JSONArray();

        for(ACMEIdentifier identifier: acmeIdentifiers){
            JSONObject identifierObj = new JSONObject();
            identifierObj.put("type", identifier.getType());
            identifierObj.put("value", identifier.getValue());
            respIdentifiersArr.put(identifierObj);

            //Unique value for each domain
            String authorizationId = Crypto.hashStringSHA256(identifier.getType() + "." + identifier.getType() + "." + DateTools.formatDateForACME(new Date()));

            //TODO: Add authorizations to Database

            respAuthorizationsArr.put(getApiURL() + "/acme/authz/" + authorizationId);
        }



        returnObj.put("status","pending");
        returnObj.put("expires", DateTools.formatDateForACME(new Date()));
        returnObj.put("notBefore", DateTools.formatDateForACME(new Date()));
        returnObj.put("notAfter", DateTools.formatDateForACME(new Date()));
        returnObj.put("identifiers",respIdentifiersArr);
        returnObj.put("authorizations",respAuthorizationsArr);
        returnObj.put("finalize", getApiURL() + "/acme/order/" + orderId + "/finalize");


        return returnObj.toString();

    };
    /**
     * Ownership verification endpoint
     *
     * URL: /acme/authz/5429d8d61a406 ...
     */
    public static Route authz = (request, response) -> {
        String authorizationId = request.params("authorizationId");

        response.header("Content-Type","application/jose+json");
        response.status(200);



        ACMEIdentifier identifier = new ACMEIdentifier("dns","www.example.org");

        JSONObject identifierObj = new JSONObject();
        identifierObj.put("type",identifier.getType());
        identifierObj.put("value",identifier.getType());

        JSONArray challengeArr = new JSONArray();

        JSONObject challengeHTTP = new JSONObject();
        challengeHTTP.put("type","http-01"); //
        challengeHTTP.put("url",getApiURL() + "/acme/chall/prV_B7yEyA4");
        challengeHTTP.put("token","DGyRejmCefe7v4NfDGDKfA"); // This URL has to be placed on the server
        challengeArr.put(challengeHTTP);


        JSONObject returnObj = new JSONObject();

        returnObj.put("status", "pending");
        returnObj.put("expires", DateTools.formatDateForACME(new Date()));
        returnObj.put("identifier", identifierObj);
        returnObj.put("challenges", challengeArr);




        return returnObj.toString();
    };

    /**
     * Get the ACME Server URL, reachable from other Hosts
     *
     * @return Full url (including HTTPS prefix) and port to this server
     */
    public static String getApiURL() {
        return "https://" + Main.acmeThisServerDNSName + ":" + Main.acmeThisServerAPIPort;
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

        /*
Payload:
        {
          "termsOfServiceAgreed": true,
          "contact": [
            "mailto:user@example.com"
          ]
         }


         */
        JSONObject reqBodyObj = new JSONObject(request.body());

        //Payload is Base64 Encoded
        JSONObject reqBodyPayloadObj = new JSONObject(Base64Tools.decodeBase64(reqBodyObj.getString("payload")));

        boolean reqPayloadTermsOfServiceAgreed = reqBodyPayloadObj.getBoolean("termsOfServiceAgreed");
        String reqPayloadContactEmail = "";
        // Has email? (This can be updated later)
        if (reqBodyPayloadObj.has("contact")) {
            reqPayloadContactEmail = reqBodyPayloadObj.getJSONArray("contact").getString(0);
            reqPayloadContactEmail = reqPayloadContactEmail.replace("mailto:", "");
        }


        // TODO: Create new account in database
        // https://ietf-wg-acme.github.io/acme/draft-ietf-acme-acme.html#rfc.section.7.3


        String accountId = "1";


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
