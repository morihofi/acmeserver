package de.morihofi.acmeserver.certificate.acme.challenges;

import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.tools.base64.Base64Tools;
import de.morihofi.acmeserver.tools.certificate.PemUtil;
import de.morihofi.acmeserver.tools.crypto.AcmeTokenCryptography;
import de.morihofi.acmeserver.tools.crypto.Hashing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.security.*;

public class DNSChallenge {

    /**
     * Provides functionality for handling DNS challenges in the ACME (Automated Certificate Management Environment) protocol.
     * This class includes methods for validating DNS challenges by querying DNS TXT records and comparing them to expected values.
     * It is designed to validate domain control by ensuring that DNS records contain specific tokens.
     */
    public static final Logger log = LogManager.getLogger(DNSChallenge.class);

    private DNSChallenge(){}

    /**
     * Validates a DNS challenge by querying DNS TXT records for the specified domain.
     * The method checks if the TXT records contain a token value that matches the expected value derived from the public key
     * of an ACME account.
     *
     * @param token The token value associated with the ACME challenge.
     * @param domain The domain for which the ACME challenge is being validated.
     * @param acmeAccount The ACME account containing the public key used to derive the expected token value.
     * @return {@code true} if the DNS challenge validation succeeds, otherwise {@code false}.
     * @throws IOException If an I/O error occurs during DNS query.
     * @throws GeneralSecurityException If a security-related error occurs.
     */
    public static ChallengeResult check(String token, String domain, ACMEAccount acmeAccount) throws IOException, GeneralSecurityException {
        boolean passed = false;
        String lastError = "";

        String dnsExpectedValue = getDigest(token, PemUtil.readPublicKeyFromPem(acmeAccount.getPublicKeyPEM()));

        try {

            // Abfrage des TXT-Eintrags für die ACME Challenge
            Lookup lookup = new Lookup("_acme-challenge." + domain, Type.TXT);
            lookup.run();
            String txtValue = "";
            if (lookup.getResult() == Lookup.SUCCESSFUL) {
                // Check TXT-Entries
                for (Record dnsRecord : lookup.getAnswers()) {
                    TXTRecord txt = (TXTRecord) dnsRecord;
                    for (Object value : txt.getStrings()) {
                        txtValue = value.toString();
                        if (txtValue.equals(dnsExpectedValue)) {
                            return new ChallengeResult(true, "");
                        }else {
                            lastError = "TXT record value doesn't match";
                            log.error(" TXT record value doesn't match. Expected: {} but got {}", dnsExpectedValue, txtValue);
                        }
                    }
                }
                log.error("DNS Challenge validation failed for challenge domain {}. TXT record not found", ("_acme-challenge." + domain));


            }else {
                log.error("DNS Challenge validation failed for challenge domain {}. Lookup wasn't successful.", ("_acme-challenge." + domain));

            }

        } catch (TextParseException e) {
            log.error("Error parsing domain name {}", domain, e);
            lastError = "Error parsing domain name";
        } catch (Exception e) {
            log.error("DNS Challenge failed for domain {}", domain, e);
            lastError = "Unknown exception. Server logs show more information";
        }

        return new ChallengeResult(false, lastError);
    }


    /**
     * Returns the digest string to be set in the domain's {@code _acme-challenge} TXT
     * record.
     */
    public static String getDigest(String token, PublicKey pk) {
        return Base64Tools.base64UrlEncode(Hashing.sha256hash(AcmeTokenCryptography.keyAuthorizationFor(token,pk)));
    }


}
