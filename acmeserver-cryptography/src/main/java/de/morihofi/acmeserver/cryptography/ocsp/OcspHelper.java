/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.cryptography.ocsp;


import de.morihofi.acmeserver.cryptography.keys.KeyHelper;
import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import de.morihofi.acmeserver.types.database.entities.AcmeOrder;
import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.cryptography.revoke.RevokedCertificate;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.ocsp.*;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.*;
import java.util.Date;

/**
 * OCSP Utility class
 */
@Slf4j
public class OcspHelper {

    /**
     * Processes an OCSP (Online Certificate Status Protocol) request for a given
     * certificate serial number. The certificate status is resolved directly from
     * the database and the appropriate OCSP response is generated.
     *
     * @param serialNumber The serial number of the certificate for which the OCSP response is requested.
     * @param provisioner  Provisioner Instance
     * @return An OCSPResp object representing the OCSP response for the given certificate.
     * @throws OCSPException                if there is an issue with OCSP processing.
     * @throws CertificateEncodingException if there is an issue with encoding certificates.
     * @throws OperatorCreationException    if there is an issue with operator creation.
     * @throws NoSuchAlgorithmException     if there is an issue with signing algorithm.
     * @throws UnrecoverableKeyException    if there is an issue recovering the key.
     * @throws KeyStoreException            if there is an issue with the keystore.
     */
    public static OCSPResp processOCSPRequest(BigInteger serialNumber, @NonNull AcmeProvisioner provisioner, @NonNull IServerInstance serverInstance) throws
            OCSPException, CertificateEncodingException, OperatorCreationException, KeyStoreException,
            UnrecoverableKeyException, NoSuchAlgorithmException {

        RevokedCertificate rc = AcmeOrder.getRevokedCertificate(serialNumber, provisioner.getName(), serverInstance);
        CertificateStatus certStatus;
        if (rc != null) {
            certStatus = new RevokedStatus(rc.revocationDate(), rc.revocationReason());
        } else {
            certStatus = CertificateStatus.GOOD;
        }

        log.info("Status for serial number {} is: {}", serialNumber, (certStatus != CertificateStatus.GOOD ? "revoked" : "valid"));

        ICryptoStoreManager csm = serverInstance.getCryptoStoreManager();

        X509Certificate caCert = provisioner.getIntermediateCaCertificate(csm);
        KeyPair caKeyPair = provisioner.getIntermediateCaKeyPair(csm);

        // Creating the OCSP response
        SubjectPublicKeyInfo caPublicKeyInfo = SubjectPublicKeyInfo.getInstance(caCert.getPublicKey().getEncoded());
        DigestCalculator digCalc = new JcaDigestCalculatorProviderBuilder().build().get(CertificateID.HASH_SHA1);
        BasicOCSPRespBuilder respBuilder = new BasicOCSPRespBuilder(caPublicKeyInfo, digCalc);

        respBuilder.addResponse(new CertificateID(digCalc,
                new JcaX509CertificateHolder(caCert), serialNumber), certStatus);

        // Creating and signing the OCSP response
        BasicOCSPResp basicResp = respBuilder.build(
                new JcaContentSignerBuilder(KeyHelper.getSignatureAlgorithmBasedOnKeyType(caKeyPair.getPrivate())).build(
                        caKeyPair.getPrivate()),
                new X509CertificateHolder[]{new JcaX509CertificateHolder(caCert)},
                new Date());

        return new OCSPRespBuilder().build(OCSPRespBuilder.SUCCESSFUL, basicResp);
    }
}
