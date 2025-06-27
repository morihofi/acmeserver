package de.morihofi.certgine.utils.network.dns;

import de.morihofi.certgine.types.config.network.DNSConfig;
import de.morihofi.certgine.types.intf.network.INetworkClient;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.xbill.DNS.CAARecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import java.util.List;

/**
 * Utility class for performing CAA lookups in accordance with RFC 6844.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CAAValidator {

    /**
     * Determines if issuance is permitted for the given domain based on CAA records.
     *
     * @param domain       Domain to check.
     * @param caDomain     Domain name of this CA used in CAA issue tags.
     * @param dnsConfig    DNS configuration of the server.
     * @param networkClient Network client for performing lookups.
     * @return {@code true} if issuance is allowed, otherwise {@code false}.
     */
    public static boolean isIssuanceAllowed(@NonNull String domain, @NonNull String caDomain,
                                            @NonNull DNSConfig dnsConfig, @NonNull INetworkClient networkClient) {
        List<Record> records;
        if (dnsConfig.getDohEnabled()) {
            records = DNSLookup.performDoHLookup(domain + ".", Type.CAA, networkClient.getDoHClient());
        } else {
            records = DNSLookup.performDnsServerLookup(domain + ".", Type.CAA, dnsConfig.getDnsServers());
        }
        return evaluateCaaRecords(records, caDomain);
    }

    /**
     * Evaluates the provided CAA records.
     *
     * @param records   List of DNS records.
     * @param caDomain  Domain name of this CA.
     * @return {@code true} if issuance is allowed.
     */
    public static boolean evaluateCaaRecords(List<Record> records, @NonNull String caDomain) {
        if (records == null || records.isEmpty()) {
            return true; // No CAA records found -> issuance allowed
        }

        boolean allowed = false;
        for (Record r : records) {
            if (r instanceof CAARecord caa) {
                String tag = caa.getTag();
                String value = caa.getValue();
                if (("issue".equalsIgnoreCase(tag) || "issuewild".equalsIgnoreCase(tag)) &&
                        value.equalsIgnoreCase(caDomain)) {
                    allowed = true;
                } else if ("issue".equalsIgnoreCase(tag) || "issuewild".equalsIgnoreCase(tag)) {
                    // Explicitly forbids this CA
                    log.info("Domain {} contains CAA record for different CA {}", caa.getName(), value);
                    return false;
                }
            }
        }
        return allowed;
    }
}
