package de.morihofi.acmeserver.certificate;

import de.morihofi.acmeserver.certificate.tools.Base64Tools;
import de.morihofi.acmeserver.certificate.tools.CertTools;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;

public class JWSTools {

    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        // Laden des privaten Schlüssels aus der Datei

        KeyPair keyPair = CertTools.generateRSAKeyPair(4096);

        // Erstellen des Payloads
        JSONObject payload = new JSONObject();
        payload.put("name", "John Doe");
        payload.put("age", 30);

        // Erstellen des JWS-Headers
        JSONObject header = new JSONObject();
        header.put("alg", "RS256");
        header.put("jwk", Base64Tools.encodeBase64(keyPair.getPublic().getEncoded()));

        // Codieren des Payloads und des JWS-Headers
        String encodedHeader = Base64Tools.encodeBase64(header.toString().getBytes(StandardCharsets.UTF_8));
        String encodedPayload = Base64Tools.encodeBase64(payload.toString().getBytes(StandardCharsets.UTF_8));
        String encodedSigningInput = encodedHeader + "." + encodedPayload;

        // Erstellen des SHA-256-Hashs
        byte[] signingInputBytes = encodedSigningInput.getBytes(StandardCharsets.UTF_8);
        SHA256.Digest digest = new SHA256.Digest();
        byte[] hash = digest.digest(signingInputBytes);

// Signieren des Hashs
        Signature signature = Signature.getInstance("SHA256withRSA", new BouncyCastleProvider());
        signature.initSign(keyPair.getPrivate());
        signature.update(hash);
        byte[] signatureBytes = signature.sign();

// Erstellen des JWS
        JSONObject jws = new JSONObject();
        jws.put("protected", encodedHeader);
        jws.put("payload", encodedPayload);
        jws.put("signature", Base64Tools.encodeBase64(signatureBytes));

        System.out.println(jws.toString());
    }
}
