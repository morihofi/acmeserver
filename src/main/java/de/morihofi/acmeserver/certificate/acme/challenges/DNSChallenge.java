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

    public static final Logger log = LogManager.getLogger(DNSChallenge.class);

    private DNSChallenge(){

    }

    /**
     * Checks the validity of an ACME (Automated Certificate Management Environment) challenge by querying DNS TXT records
     * for the specified domain and comparing them to the expected token value derived from the public key of an ACME account.
     *
     * @param token      The token value associated with the ACME challenge.
     * @param domain     The domain for which the ACME challenge is being validated.
     * @param acmeAccount The ACME account containing the public key used to derive the expected token value.
     * @return True if the DNS challenge validation succeeds, indicating that the TXT record matches the expected token value;
     *         otherwise, false.
     * @throws IOException             if there is an issue with input/output operations.
     * @throws GeneralSecurityException if there is a general security-related issue.
     */
    public static boolean check(String token, String domain, ACMEAccount acmeAccount) throws IOException, GeneralSecurityException {
        boolean passed = false;

        String dnsExpectedValue = getDigest(token, PemUtil.readPublicKeyFromPem(acmeAccount.getPublicKeyPEM()));

        try {
            // Abfrage des TXT-Eintrags für die ACME Challenge
            Lookup lookup = new Lookup("_acme-challenge." + domain, Type.TXT);
            lookup.run();

            if (lookup.getResult() == Lookup.SUCCESSFUL) {
                // Check TXT-Entries
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
                log.error("DNS Challenge validation failed for domain \"_acme-challenge." + domain + "\". TXT record not found or value doesn't match. Expected: " + dnsExpectedValue);
            }

        } catch (TextParseException e) {
            log.error("Error parsing domain name \"" + domain + "\"", e);
        } catch (Exception e) {
            log.error("DNS Challenge failed for domain \"" + domain + "\"", e);
        }

        return passed;
    }


    /**
     * Returns the digest string to be set in the domain's {@code _acme-challenge} TXT
     * record.
     */
    public static String getDigest(String token, PublicKey pk) {
        return Base64Tools.base64UrlEncode(Hashing.sha256hash(AcmeTokenCryptography.keyAuthorizationFor(token,pk)));
    }


}
