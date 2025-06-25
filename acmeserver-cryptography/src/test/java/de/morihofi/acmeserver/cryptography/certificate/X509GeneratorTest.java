package de.morihofi.acmeserver.cryptography.certificate;

import de.morihofi.acmeserver.types.database.entities.authority.CertificateMetadata;
import org.bouncycastle.asn1.x500.X500Name;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class X509GeneratorTest {

    private X500Name invokeToX500(CertificateMetadata meta) throws Exception {
        Method m = X509Generator.class.getDeclaredMethod("toX500", CertificateMetadata.class, String.class);
        m.setAccessible(true);
        return (X500Name) m.invoke(null, meta, "error");
    }

    @Test
    @DisplayName("toX500 builds name string")
    void testToX500() throws Exception {
        CertificateMetadata meta = CertificateMetadata.builder()
                .commonName("CN")
                .organisation("Org")
                .countryCode("DE")
                .build();
        X500Name name = invokeToX500(meta);
        assertEquals("CN=CN,O=Org,C=DE", name.toString());
    }
}
