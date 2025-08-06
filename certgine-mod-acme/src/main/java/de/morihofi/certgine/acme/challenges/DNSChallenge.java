/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.challenges;

import de.morihofi.certgine.acme.types.entities.AcmeAccount;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.utils.base64.Base64Tools;
import de.morihofi.certgine.cryptography.pem.PemUtil;
import de.morihofi.certgine.cryptography.acme.AcmeTokenCryptography;
import de.morihofi.certgine.utils.crypto.Hashing;
import de.morihofi.certgine.utils.network.dns.DNSLookup;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.List;

/**
 * Provides functionality for handling DNS challenges in the ACME (Automated Certificate Management Environment) protocol. This class
 * includes methods for validating DNS challenges by querying DNS TXT records and comparing them to expected values. It is designed to
 * validate domain control by ensuring that DNS records contain specific tokens.
 */
@Slf4j
public class DNSChallenge {

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
    @NonNull
    public static ChallengeResult check(@NonNull String token, @NonNull String domain, @NonNull AcmeAccount acmeAccount, @NonNull IServerInstance serverInstance) throws IOException, GeneralSecurityException {
        String lastError = "";

        String dnsExpectedValue = getDigest(token, PemUtil.readPublicKeyFromPem(acmeAccount.getPublicKeyPEM()));
        final String lookupDomain = "_acme-challenge." + domain;
        try {
            log.info("Looking up TXT value on domain {}", lookupDomain);

            // Perform DNS lookup
            List<Record> dnsRecords;
            if (serverInstance.getAppConfig().getNetwork().getDnsConfig().getDohEnabled()) {
                // Using DoHClient for the DNS lookup
                log.info("Using DNS over HTTPS Lookup");
                dnsRecords = DNSLookup.performDoHLookup(lookupDomain + ".", Type.TXT, serverInstance.getNetworkClient().getDoHClient());
            } else {
                // Using standard DNS lookup
                log.info("Using DNS default Lookup");
                dnsRecords = DNSLookup.performDnsServerLookup(lookupDomain + ".", Type.TXT, serverInstance.getAppConfig().getNetwork().getDnsConfig()
                        .getDnsServers());
            }

            String txtValue;
            if (dnsRecords != null && !dnsRecords.isEmpty()) {
                // Check TXT-Entries
                for (Record dnsRecord : dnsRecords) {
                    TXTRecord txt = (TXTRecord) dnsRecord;
                    for (Object value : txt.getStrings()) {
                        txtValue = value.toString();
                        if (txtValue.equals(dnsExpectedValue)) {
                            return new ChallengeResult(true, "");
                        } else {
                            lastError = "TXT record value doesn't match";
                            log.error("TXT record value doesn't match. Expected {} but got {}", dnsExpectedValue, txtValue);
                        }
                    }
                }
                log.error("DNS Challenge validation failed for challenge domain {}. TXT record not found", lookupDomain);
            } else {
                log.error("DNS Challenge validation failed for challenge domain {}. Lookup wasn't successful.", lookupDomain);
            }
        } catch (Exception e) {
            log.error("DNS Challenge failed for domain {}", domain, e);
            lastError = "Unknown exception. Server logs show more information";
        }

        return new ChallengeResult(false, lastError);
    }

    /**
     * Returns the digest string to be set in the domain's {@code _acme-challenge} TXT record.
     */
    @NonNull
    public static String getDigest(@NonNull String token, @NonNull PublicKey pk) {
        return Base64Tools.base64UrlEncode(Hashing.sha256hash(AcmeTokenCryptography.keyAuthorizationFor(token, pk)));
    }

    /**
     * Setting the DNS resolver manually
     *
     * @param dnsServerIP DNS Server to use
     * @throws UnknownHostException host is unknown
     */
    public static void setManualDNSResolver(@NonNull String dnsServerIP) throws UnknownHostException {
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
