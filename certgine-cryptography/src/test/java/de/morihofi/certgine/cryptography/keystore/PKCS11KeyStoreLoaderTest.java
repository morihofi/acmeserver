package de.morihofi.certgine.cryptography.keystore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PKCS11KeyStoreLoaderTest {

    @Test
    @DisplayName("loadPKCS11Keystore throws for invalid library")
    void testLoadPkcs11() {
        assertThrows(Throwable.class,
                () -> PKCS11KeyStoreLoader.loadPKCS11Keystore("1234".toCharArray(), 0, "/nonexistent"));
    }
}
