package de.morihofi.certgine.cryptography.csr;

import de.morihofi.certgine.types.dns.DnsIdentifier;
import de.morihofi.certgine.utils.base64.Base64Tools;
import lombok.NonNull;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility methods for extracting information from Certificate Signing Requests.
 */
public final class CsrDataUtil {

    private CsrDataUtil() {
    }

    /**
     * Extracts domain names and IP addresses from a CSR encoded in Base64URL format.
     *
     * @param csr CSR encoded as Base64URL string
     * @return set of identifiers contained in the CSR
     * @throws IOException if the CSR cannot be parsed
     */
    @NonNull
    public static Set<@NonNull DnsIdentifier> getDomainsAndIPsFromCSR(@NonNull String csr) throws IOException {
        byte[] csrBytes = Base64Tools.decodeBase64URLAsBytes(csr);
        PKCS10CertificationRequest certRequest = new PKCS10CertificationRequest(csrBytes);

        Set<DnsIdentifier> domainAndIpList = new HashSet<>();

        X500Name subject = certRequest.getSubject();
        RDN[] cnRDNs = subject.getRDNs(BCStyle.CN);
        if (cnRDNs.length != 0) {
            String commonName = cnRDNs[0].getFirst().getValue().toString();
            domainAndIpList.add(new DnsIdentifier(DnsIdentifier.IDENTIFIER_TYPE.DNS, commonName));
        }

        Extension sanExtension = certRequest.getRequestedExtensions().getExtension(Extension.subjectAlternativeName);
        if (sanExtension != null) {
            GeneralNames san = GeneralNames.getInstance(sanExtension.getParsedValue());
            for (GeneralName name : san.getNames()) {
                if (name.getTagNo() == GeneralName.dNSName) {
                    domainAndIpList.add(new DnsIdentifier(DnsIdentifier.IDENTIFIER_TYPE.DNS, name.getName().toString()));
                } else if (name.getTagNo() == GeneralName.iPAddress) {
                    byte[] ip = ASN1OctetString.getInstance(name.getName()).getOctets();
                    domainAndIpList.add(new DnsIdentifier(DnsIdentifier.IDENTIFIER_TYPE.IP, convertToIP(ip)));
                }
            }
        }

        return domainAndIpList;
    }

    @NonNull
    private static String convertToIP(byte[] ip) throws UnknownHostException {
        InetAddress ipAddress = InetAddress.getByAddress(ip);
        return ipAddress.getHostAddress();
    }
}
