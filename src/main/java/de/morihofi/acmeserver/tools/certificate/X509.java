package de.morihofi.acmeserver.tools.certificate;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class X509 {

    private X509() {

    }

    /**
     * Extracts the X.500 Distinguished Name (DN) from an X.509 certificate.
     *
     * @param cert The X.509 certificate from which to extract the DN.
     * @return An X500Name object representing the subject DN of the certificate.
     * @throws CertificateEncodingException If there is an issue encoding the certificate or extracting the DN.
     */
    public static X500Name getX500NameFromX509Certificate(X509Certificate cert) throws CertificateEncodingException {
        return new JcaX509CertificateHolder(cert).getSubject();
    }

    /**
     * Converts a byte array representing an X.509 certificate into an X.509 certificate object.
     *
     * @param certificateBytes The byte array containing the X.509 certificate data.
     * @return An X509Certificate object representing the parsed X.509 certificate.
     * @throws CertificateException If there is an issue parsing the X.509 certificate from the byte array.
     */
    public static X509Certificate convertToX509Cert(byte[] certificateBytes) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certificateBytes));
    }

}
