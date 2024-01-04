package de.morihofi.acmeserver.postsetup;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * A utility class for obtaining the Fully Qualified Domain Name (FQDN) of the local machine.
 */
public class NetworkHelper {

    /**
     * Retrieves the Fully Qualified Domain Name (FQDN) of the local machine.
     *
     * @return The FQDN of the local machine, or null if it cannot be determined.
     * @throws SocketException If there is an issue with network interfaces.
     */
    public static String getLocalFqdn() throws SocketException {
        String fqdn = null;
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface ni = networkInterfaces.nextElement();
            if (!ni.isLoopback() && ni.isUp()) {
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // Ignore local addresses
                    if (!addr.isLinkLocalAddress() && !addr.isLoopbackAddress() && !addr.isSiteLocalAddress()) {
                        fqdn = addr.getCanonicalHostName();
                        break;
                    }
                }
                if (fqdn != null) {
                    break;
                }
            }
        }

        return fqdn;
    }
}
