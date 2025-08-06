package de.morihofi.certgine.utils.network.dns;

import java.net.InetAddress;
import java.util.List;

/**
 * Strategy interface for resolving DNS records.
 */
public interface ResolverStrategy {
    /**
     * Resolves the given host for the specified DNS record type.
     *
     * @param host       the host name to resolve
     * @param recordType the DNS record type (e.g., A, AAAA)
     * @return list of resolved {@link InetAddress} entries
     */
    List<InetAddress> resolve(String host, int recordType);
}
