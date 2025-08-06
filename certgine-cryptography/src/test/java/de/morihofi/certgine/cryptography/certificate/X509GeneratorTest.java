/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.cryptography.certificate;

import de.morihofi.certgine.types.database.entities.authority.CertificateMetadata;
import org.bouncycastle.asn1.x500.X500Name;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
