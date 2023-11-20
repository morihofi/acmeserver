package de.morihofi.acmeserver.certificate.acme.challenges;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.tools.PemUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.openssl.PEMParser;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.io.StringReader;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DNSChallenge {

    public static final Logger log = LogManager.getLogger(DNSChallenge.class);

    public static boolean check(String token, String domain, ACMEAccount acmeAccount) throws IOException, GeneralSecurityException {
        boolean passed = false;

        String dnsExpectedValue = getDigest(token, PemUtil.readPublicKeyFromPem(acmeAccount.getPublicKeyPEM()));

        try {
            // Abfrage des TXT-Eintrags für die ACME Challenge
            Lookup lookup = new Lookup("_acme-challenge." + domain, Type.TXT);
            lookup.run();

            if (lookup.getResult() == Lookup.SUCCESSFUL) {
                // Überprüfen der TXT-Einträge
                for (Record record : lookup.getAnswers()) {
                    TXTRecord txt = (TXTRecord) record;
                    for (Object value : txt.getStrings()) {
                        String txtValue = value.toString();
                        if (txtValue.equals(dnsExpectedValue)) {
                            passed = true;
                            log.info("DNS Challenge has validated for domain \"" + domain + "\"");
                            break;
                        }
                    }
                    if (passed) {
                        break;
                    }
                }
            }

            if (!passed) {
                log.error("DNS Challenge validation failed for domain \"" + domain + "\". TXT record not found or value doesn't match. Expected: " + dnsExpectedValue);
            }

        } catch (TextParseException e) {
            log.error("Error parsing domain name \"" + domain + "\"", e);
        } catch (Exception e) {
            log.error("DNS Challenge failed for domain \"" + domain + "\"", e);
        }

        return passed;
    }


    /**
     * Computes a thumbprint of the given public key.
     *
     * @param key
     *         {@link PublicKey} to get the thumbprint of
     * @return Thumbprint of the key
     */
    public static byte[] thumbprint(PublicKey key) {
        try {
            var jwk = PublicJsonWebKey.Factory.newPublicJwk(key);
            return jwk.calculateThumbprint("SHA-256");
        } catch (JoseException ex) {
            throw new IllegalArgumentException("Bad public key", ex);
        }
    }

    /**
     * Returns the digest string to be set in the domain's {@code _acme-challenge} TXT
     * record.
     */
    public static String getDigest(String token, PublicKey pk) {
        return base64UrlEncode(sha256hash(keyAuthorizationFor(token,pk)));
    }

    private static final java.util.Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    /**
     * Base64 encodes the given byte array, using URL style encoding.
     *
     * @param data
     *            byte array to base64 encode
     * @return base64 encoded string
     */
    public static String base64UrlEncode(byte[] data) {
        return URL_ENCODER.encodeToString(data);
    }

    /**
     * Computes a SHA-256 hash of the given string.
     *
     * @param z
     *            String to hash
     * @return Hash
     */
    public static byte[] sha256hash(String z) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            md.update(z.getBytes(UTF_8));
            return md.digest();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalArgumentException("Could not compute hash", ex);
        }
    }

    /**
     * Computes the key authorization for the given token.
     * <p>
     * The default is {@code token + '.' + base64url(jwkThumbprint)}. Subclasses may
     * override this method if a different algorithm is used.
     *
     * @param token
     *         Token to be used
     * @return Key Authorization string for that token
     */
    protected static String keyAuthorizationFor(String token, PublicKey pk) {
        return token + '.' + base64UrlEncode(thumbprint(pk));
    }

}
