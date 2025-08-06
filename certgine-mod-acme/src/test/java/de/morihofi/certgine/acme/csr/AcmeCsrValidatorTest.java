package de.morihofi.certgine.acme.csr;

import de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifierChallenge;
import de.morihofi.certgine.acme.types.entities.enums.AcmeStatus;
import de.morihofi.certgine.types.dns.DnsIdentifier;
import de.morihofi.certgine.types.exception.exceptions.ACMEBadCsrException;
import de.morihofi.certgine.utils.base64.Base64Tools;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcmeCsrValidatorTest {

    @BeforeAll
    static void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static String createCsr() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(512);
        KeyPair kp = kpg.generateKeyPair();
        X500Name subject = new X500Name("CN=example.com");
        PKCS10CertificationRequestBuilder builder = new JcaPKCS10CertificationRequestBuilder(subject, kp.getPublic());
        ExtensionsGenerator extGen = new ExtensionsGenerator();
        GeneralName[] names = new GeneralName[]{
                new GeneralName(GeneralName.dNSName, "example.com"),
                new GeneralName(GeneralName.iPAddress, "127.0.0.1")
        };
        extGen.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(names));
        builder.addAttribute(org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(kp.getPrivate());
        PKCS10CertificationRequest csr = builder.build(signer);
        return Base64Tools.base64UrlEncode(csr.getEncoded());
    }

    @Test
    @DisplayName("getCsrIdentifiersAndVerifyWithIdentifiers checks identifiers")
    void testGetCsrIdentifiersAndVerify() throws Exception {
        String csr = createCsr();
        de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier dns = new de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier("dns", "example.com");
        AcmeOrderIdentifierChallenge ch1 = new AcmeOrderIdentifierChallenge(null, dns, "cid1", "tok");
        ch1.setStatus(AcmeStatus.VALID);
        dns.setChallenges(List.of(ch1));
        de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier ip = new de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier("ip", "127.0.0.1");
        AcmeOrderIdentifierChallenge ch2 = new AcmeOrderIdentifierChallenge(null, ip, "cid2", "tok");
        ch2.setStatus(AcmeStatus.VALID);
        ip.setChallenges(List.of(ch2));
        Set<DnsIdentifier> result = AcmeCsrValidator.getCsrIdentifiersAndVerifyWithIdentifiers(csr, List.of(dns, ip));
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("getCsrIdentifiersAndVerifyWithIdentifiers fails on mismatch")
    void testGetCsrIdentifiersAndVerifyMismatch() throws Exception {
        String csr = createCsr();
        de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier id = new de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier("dns", "other.com");
        AcmeOrderIdentifierChallenge challenge = new AcmeOrderIdentifierChallenge(null, id, "cid", "tok");
        challenge.setStatus(AcmeStatus.VALID);
        id.setChallenges(List.of(challenge));
        assertThrows(ACMEBadCsrException.class,
                () -> AcmeCsrValidator.getCsrIdentifiersAndVerifyWithIdentifiers(csr, List.of(id)));
    }
}
