package de.morihofi.acmeserver.certificate;

import de.morihofi.acmeserver.Main;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.json.JSONObject;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

public class JWSTestVerifyExample {
    public static void checkSignature(JSONObject jsonObject) throws Exception {

        // Parse the JWS object

        String encodedHeader = jsonObject.getString("protected");
        String encodedPayload = jsonObject.getString("payload");
        String encodedSignature = jsonObject.getString("signature");


        String decodedHeader = decodeBase64URL(encodedHeader);
        String decodedPayload = decodeBase64URL(encodedPayload);


        // Parse the header and payload as JSON objects
        JSONObject header = new JSONObject(decodedHeader);
        JSONObject payload = new JSONObject(decodedPayload);

        // Extract the protected header
        String protectedHeader = header.optString("protected");

        // Extract the signature algorithm
        String alg = header.optString("alg");
        if (!"RS256".equals(alg)) {
            throw new IllegalArgumentException("Unsupported signature algorithm: " + alg);
        }

        // Extract the public key from the protected header
        //String jwk = header.optString("jwk");
        //JSONObject jwkObject = new JSONObject(jwk);
        byte[] modulus = Main.acmeSignatureKeyPair.getPublic().getEncoded(); //Base64.decode(jwkObject.getString("n"));
        //byte[] exponent = Base64.decode(jwkObject.getString("e"));
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(
                ("-----BEGIN PUBLIC KEY-----\n" + Base64.toBase64String(modulus) + "\n-----END PUBLIC KEY-----").getBytes());
        KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        // Verify the signature
        Signature signature = Signature.getInstance("SHA256withRSA", "BC");
        signature.initVerify(publicKey);
        signature.update((protectedHeader + "." + encodedPayload).getBytes());
        boolean verified = signature.verify(Base64.decode(encodedSignature.replace("-", "+").replace("_", "/")));
        if (!verified) {
            throw new Exception("Invalid signature");
        }

        // Print the payload
        System.out.println("Payload: " + payload.toString());
    }

    public static String decodeBase64URL(String stringToDecode) {

        // Getting decoder
        java.util.Base64.Decoder decoder = java.util.Base64.getUrlDecoder();
        // Decoding URl
        String dStr = new String(decoder.decode(stringToDecode));
        System.out.println("Decoded URL: " + dStr);

        return dStr;
    }

    public static String encodedBase64URL(String stringToEncode){

        // Getting encoder
        java.util.Base64.Encoder encoder = java.util.Base64.getUrlEncoder();
        // Encoding URL
        String eStr = encoder.encodeToString(stringToEncode.getBytes());
        System.out.println("Encoded URL: " + eStr);

        return eStr;
    }




}
