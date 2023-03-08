package de.morihofi.acmeserver.certificate.acmeapi;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.certificate.tools.Base64Tools;
import de.morihofi.acmeserver.certificate.tools.Crypto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Route;

public class AcmeAPI {


    public static Route newNonce = (request, response) -> {


        response.status(200);
        response.header("Cache-Control", "no-store");
        response.header("Link", "<" + getApiURL() + "/directory" +">;rel=\"index\"");

        response.header("Replay-Nonce", Crypto.createNonce());

        return "";
    };

    /**
     * Get the ACME Server URL, reachable from other Hosts
     *
     * @return Full url (including HTTPS prefix) and port to this server
     */
    public static String getApiURL(){
        return "https://" + Main.acmeThisServerDNSName + ":" + Main.acmeThisServerAPIPort;
    }

    /**
     * Get ACME Directory Info Endpoint
     *
     * URL: /directory
     */
    public static Route directoryEndpoint = (request, response) -> {
        // Response is JSON
        response.header("Content-Type","application/json");

        JSONObject responseJSON = new JSONObject();

        JSONObject metaObject = new JSONObject();
        metaObject.put("website", Main.acmeMetaWebsite);
        metaObject.put("termsOfService", Main.acmeMetaTermsOfService);



        responseJSON.put("meta",metaObject);
        responseJSON.put("newAccount",getApiURL() + "/acme/new-acct");
        responseJSON.put("newNonce",getApiURL() + "/acme/new-nonce");
        responseJSON.put("newOrder",getApiURL() + "/acme/new-order");
        responseJSON.put("revokeCert",getApiURL() + "/acme/revoke-cert");
        responseJSON.put("keyChange",getApiURL() + "/acme/key-change");




        return responseJSON.toString();
    };

    /**
     * Create a new ACME Account Endpoint
     *
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
}

         */
        JSONObject reqBodyObj = new JSONObject(request.body());

        //Payload is Base64 Encoded
        JSONObject reqBodyPayloadObj = new JSONObject(Base64Tools.decodeBase64(reqBodyObj.getString("payload")));

        boolean reqPayloadTermsOfServiceAgreed = reqBodyPayloadObj.getBoolean("termsOfServiceAgreed");
        String reqPayloadContactEmail = "";
        // Has email? (This can updated later)
        if(reqBodyPayloadObj.has("contact")){
            reqPayloadContactEmail = reqBodyPayloadObj.getJSONArray("contact").getString(0);
        }

        reqPayloadContactEmail = reqPayloadContactEmail.replace("mailto:","");

        // TODO: Create new account in database


        String accountId = "1";


        String nonce = Crypto.createNonce();
        // Response is JSON
        response.header("Content-Type","application/json");
        response.header("Location",getApiURL() + "/acme/acct/" + accountId);
        response.header("Replay-Nonce",nonce);
        response.status(201); //Created

        JSONObject responseJSON = new JSONObject();


        //Contact information
        JSONArray contactEmailsArr = new JSONArray();
        contactEmailsArr.put(reqPayloadContactEmail);

        //Key object
        JSONObject keyObj = new JSONObject();


        // Create a JWT object with an empty signature
        /*
        Claims claims = Jwts.claims();
        String jwt = Jwts.builder()
                .setHeaderParam("nonce", nonce)
                .setHeaderParam("url", getApiURL() + "/acme/new-acct")
                .setClaims(claims)
                .signWith(Main.intermediateKeyPair.getPublic(), SignatureAlgorithm.RS256)
                .compact();
*/


    //    keyObj.put("kty","RSA"); //RSA Key
    //    keyObj.put("n", jwt); //Nonce?
    //    keyObj.put("e","AQAB"); //?
    //    keyObj.put("kid", getApiURL() + "/acme/acct/" + accountId);


        responseJSON.put("status","valid");
        responseJSON.put("contact",contactEmailsArr);
        responseJSON.put("orders", getApiURL() + "/acme/acct/" + accountId +"/orders");
        responseJSON.put("key", keyObj.toString());


        return responseJSON.toString();
    };

}
