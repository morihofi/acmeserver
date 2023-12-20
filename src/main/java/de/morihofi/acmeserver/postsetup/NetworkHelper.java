package de.morihofi.acmeserver.postsetup;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetworkHelper {
    public static String getLocalFqdn() throws SocketException {
        String fqdn = null;
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface ni = networkInterfaces.nextElement();
            if (!ni.isLoopback() && ni.isUp()) {
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // Ignorieren Sie lokale Adressen
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
