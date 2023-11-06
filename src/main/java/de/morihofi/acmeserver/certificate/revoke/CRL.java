package de.morihofi.acmeserver.certificate.revoke;

import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.CRLException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

public class CRL {

    public X509CRL generateCRL(List<BigInteger> revokedSerialNumbers,
                               X509Certificate caCert,
                               PrivateKey caPrivateKey,
                               String signatureAlgorithm) throws CertificateEncodingException, CRLException, OperatorCreationException {

        // Erstellen Sie den CRL-Builder
        X509v2CRLBuilder crlBuilder = new X509v2CRLBuilder(
                new JcaX509CertificateHolder(caCert).getSubject(),
                new Date()
        );

        // Fügen Sie ein Ablaufdatum hinzu - z.B. in einem Jahr
        crlBuilder.setNextUpdate(new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L));

        // Fügen Sie die gesperrten Seriennummern hinzu
        for (BigInteger serialNumber : revokedSerialNumbers) {
            crlBuilder.addCRLEntry(serialNumber, new Date(), 0);
        }

        // Signieren Sie die CRL mit dem privaten Schlüssel des CA
        JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder(signatureAlgorithm);
        X509CRLHolder crlHolder = crlBuilder.build(signerBuilder.build(caPrivateKey));

        // Konvertieren Sie die CRL in ein Java CRL-Objekt
        JcaX509CRLConverter converter = new JcaX509CRLConverter();
        converter.setProvider(BouncyCastleProvider.PROVIDER_NAME);


        return converter.getCRL(crlHolder);
    }

}
