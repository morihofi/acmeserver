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

package de.morihofi.acmeserver.core.certificate.revokeDistribution;


import de.morihofi.acmeserver.cryptography.keys.KeyHelper;
import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.ocsp.*;
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
     * Processes an OCSP (Online Certificate Status Protocol) request for a given certificate serial number. This method checks the status
     * of the certificate using the current Certificate Revocation List (CRL) and generates an OCSP response accordingly.
     *
     * @param serialNumber The serial number of the certificate for which the OCSP response is requested.
     * @param provisioner  Provisioner Instance
     * @return An OCSPResp object representing the OCSP response for the given certificate.
     * @throws OCSPException                if there is an issue with OCSP processing.
     * @throws CRLException                 if there is an issue with CRL processing.
     * @throws CertificateEncodingException if there is an issue with encoding certificates.
     * @throws OperatorCreationException    if there is an issue with operator creation.
     * @throws NoSuchAlgorithmException     if there is an issue with signing algorithm.
     * @throws UnrecoverableKeyException    if there is an issue recovering the key.
     * @throws KeyStoreException            if there is an issue with the keystore.
     */
    public static OCSPResp processOCSPRequest(BigInteger serialNumber, @NonNull AcmeProvisioner provisioner, @NonNull IServerInstance serverInstance) throws
            OCSPException, CRLException, CertificateEncodingException, OperatorCreationException, KeyStoreException,
            UnrecoverableKeyException, NoSuchAlgorithmException {

        CertificateStatus certStatus = CrlStore.getCertificateStatus(serialNumber, provisioner.getName());

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
