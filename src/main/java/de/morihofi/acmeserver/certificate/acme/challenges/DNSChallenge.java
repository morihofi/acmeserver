package de.morihofi.acmeserver.certificate.acme.challenges;

import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.tools.base64.Base64Tools;
import de.morihofi.acmeserver.tools.certificate.PemUtil;
import de.morihofi.acmeserver.tools.crypto.AcmeTokenCryptography;
import de.morihofi.acmeserver.tools.crypto.Hashing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

/**
 * Provides functionality for handling DNS challenges in the ACME (Automated Certificate Management Environment) protocol. This class
 * includes methods for validating DNS challenges by querying DNS TXT records and comparing them to expected values. It is designed to
 * validate domain control by ensuring that DNS records contain specific tokens.
 */
public class DNSChallenge {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    /**
     * Validates a DNS challenge by querying DNS TXT records for the specified domain. The method checks if the TXT records contain a token
     * value that matches the expected value derived from the public key of an ACME account.
     *
     * @param token       The token value associated with the ACME challenge.
     * @param domain      The domain for which the ACME challenge is being validated.
     * @param acmeAccount The ACME account containing the public key used to derive the expected token value.
     * @return {@code true} if the DNS challenge validation succeeds, otherwise {@code false}.
     * @throws IOException              If an I/O error occurs during DNS query.
     * @throws GeneralSecurityException If a security-related error occurs.
     */
    public static ChallengeResult check(String token, String domain, ACMEAccount acmeAccount) throws IOException, GeneralSecurityException {
        String lastError = "";

        String dnsExpectedValue = getDigest(token, PemUtil.readPublicKeyFromPem(acmeAccount.getPublicKeyPEM()));
        String lookupDomain = "_acme-challenge." + domain;
        try {
            LOG.info("Looking up TXT value on domain {}", lookupDomain);

            // Query of the TXT entry for the ACME Challenge
            Lookup lookup = new Lookup(lookupDomain, Type.TXT);
            lookup.run();
            String txtValue;
            if (lookup.getResult() == Lookup.SUCCESSFUL) {
                // Check TXT-Entries
                for (Record dnsRecord : lookup.getAnswers()) {
                    TXTRecord txt = (TXTRecord) dnsRecord;
                    for (Object value : txt.getStrings()) {
                        txtValue = value.toString();
                        if (txtValue.equals(dnsExpectedValue)) {
                            return new ChallengeResult(true, "");
                        } else {
                            lastError = "TXT record value doesn't match";
                            LOG.error(" TXT record value doesn't match. Expected: {} but got {}", dnsExpectedValue, txtValue);
                        }
                    }
                }
                LOG.error("DNS Challenge validation failed for challenge domain {}. TXT record not found", ("_acme-challenge." + domain));
            } else {
                LOG.error("DNS Challenge validation failed for challenge domain {}. Lookup wasn't successful.",
                        ("_acme-challenge." + domain));
            }
        } catch (TextParseException e) {
            LOG.error("Error parsing domain name {}", domain, e);
            lastError = "Error parsing domain name";
        } catch (Exception e) {
            LOG.error("DNS Challenge failed for domain {}", domain, e);
            lastError = "Unknown exception. Server logs show more information";
        }

        return new ChallengeResult(false, lastError);
    }

    /**
     * Returns the digest string to be set in the domain's {@code _acme-challenge} TXT record.
     */
    public static String getDigest(String token, PublicKey pk) {
        return Base64Tools.base64UrlEncode(Hashing.sha256hash(AcmeTokenCryptography.keyAuthorizationFor(token, pk)));
    }

    /**
     * Setting the DNS resolver manually
     *
     * @param dnsServerIP DNS Server to use
     * @throws UnknownHostException host is unknown
     */
    public static void setManualDNSResolver(String dnsServerIP) throws UnknownHostException {
        // Setting the DNS resolver manually
        SimpleResolver resolver = new SimpleResolver(dnsServerIP);
        Lookup.setDefaultResolver(resolver);
    }

    /**
     * Reset to the system resolver
     */
    public static void setSystemDNSResolver() {
        // Reset to the system resolver
        // This is achieved by setting the DefaultResolver to null, since dnsjava
        // then uses the system's resolver configuration
        Lookup.setDefaultResolver(null);
    }

    private DNSChallenge() {
    }
}
