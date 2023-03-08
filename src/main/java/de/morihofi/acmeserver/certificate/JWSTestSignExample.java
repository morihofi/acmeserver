package de.morihofi.acmeserver.certificate;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.certificate.acmeapi.AcmeAPI;
import de.morihofi.acmeserver.certificate.tools.Base64Tools;
import de.morihofi.acmeserver.certificate.tools.CertTools;
import de.morihofi.acmeserver.certificate.tools.Crypto;
import de.morihofi.acmeserver.certificate.tools.KeyStoreUtils;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.json.JSONObject;

import javax.crypto.Cipher;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;

public class JWSTestSignExample {

    public static void main(String[] args) throws Exception {
        // Register the Bouncy Castle provider
        Security.addProvider(new BouncyCastleProvider());

        // Create a sample JSON object
        JSONObject payload = new JSONObject();
        payload.put("name", "Alice");
        payload.put("age", 30);

        String url = "https://google.com";
        String nonce = Crypto.createNonce();

        // Generate a key pair for signing
        KeyPair keyPair = KeyStoreUtils.openRSAKeyPairFromDirectory(Main.acmeSignatureKeyPairPath);

        // **************************
        // Begin signature the payload

        // Serialize the payload to a JSON string
        String payloadJson = payload.toString();

        // Create the JWS header
        JSONObject header = new JSONObject();
        header.put("alg", "RS256");
        header.put("typ", "JWT");

        header.put("nonce", nonce);
        header.put("url", url);

        // Serialize the JWS header to a JSON string
        String headerJson = header.toString();

        // Encode the header and payload as Base64Url strings
        String encodedHeader = Base64.toBase64String(headerJson.getBytes("UTF-8"))
                .replace("+", "-").replace("/", "_").replace("=", "");
        String encodedPayload = Base64.toBase64String(payloadJson.getBytes("UTF-8"))
                .replace("+", "-").replace("/", "_").replace("=", "");

        // Compute the JWS signature
        byte[] signature = computeRSASignature(keyPair.getPrivate(), encodedHeader + "." + encodedPayload);

        // Encode the signature as a Base64Url string
        String encodedSignature = Base64.toBase64String(signature)
                .replace("+", "-").replace("/", "_").replace("=", "");

        // Concatenate the encoded header, payload, and signature
        String jws = encodedHeader + "." + encodedPayload + "." + encodedSignature;

        JSONObject finalObj = new JSONObject();
        finalObj.put("payload", encodedPayload);
        finalObj.put("protected", encodedHeader);
        finalObj.put("signature", encodedSignature);



        // Print the resulting JWS
        System.out.println(finalObj);

        JWSTestVerifyExample.checkSignature(finalObj);
    }


    private static byte[] computeRSASignature(PrivateKey privateKey, String message) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] messageBytes = message.getBytes("UTF-8");
        byte[] signatureBytes = cipher.doFinal(messageBytes);
        return signatureBytes;
    }


}
