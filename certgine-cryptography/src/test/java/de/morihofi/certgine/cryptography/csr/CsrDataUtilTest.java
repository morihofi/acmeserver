package de.morihofi.certgine.cryptography.csr;

import de.morihofi.certgine.types.dns.DnsIdentifier;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CsrDataUtilTest {

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
    @DisplayName("getDomainsAndIPsFromCSR returns identifiers")
    void testGetDomains() throws Exception {
        String csr = createCsr();
        Set<DnsIdentifier> ids = CsrDataUtil.getDomainsAndIPsFromCSR(csr);
        assertTrue(ids.stream().anyMatch(i -> i.getValue().equals("example.com")));
        assertTrue(ids.stream().anyMatch(i -> i.getValue().equals("127.0.0.1")));
    }
}
