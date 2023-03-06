package de.morihofi.acmeserver.certificate;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

public class X509 {
    public static X500Name getX500NameFromX509Certificate(X509Certificate cert) throws CertificateEncodingException {

        X500Name x500name = new JcaX509CertificateHolder(cert).getSubject();
        // RDN cn = x500name.getRDNs(BCStyle.CN)[0];

        return x500name;
    }
}
