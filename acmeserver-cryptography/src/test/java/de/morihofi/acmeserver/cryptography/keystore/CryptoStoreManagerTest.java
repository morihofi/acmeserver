package de.morihofi.acmeserver.cryptography.keystore;

import com.google.common.jimfs.Jimfs;
import de.morihofi.acmeserver.types.cryptography.keystore.PKCS12KeyStoreConfig;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;

import static org.junit.jupiter.api.Assertions.*;

class CryptoStoreManagerTest {

    @BeforeAll
    static void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    @DisplayName("alias generation uses prefix and validates name")
    void testAliasGeneration() throws Exception {
        Path tmp = Files.createTempDirectory("ks").resolve("store.p12");
        CryptoStoreManager mgr = assertDoesNotThrow(() -> new CryptoStoreManager(
                new PKCS12KeyStoreConfig(tmp, "pass".toCharArray())));
        assertEquals("intermediateCA_test", mgr.getKeyStoreAliasForProvisionerIntermediate("test"));
        assertThrows(IllegalArgumentException.class,
                () -> mgr.getKeyStoreAliasForProvisionerIntermediate("bad name"));
    }

    @Test
    @DisplayName("constructor creates PKCS12 keystore")
    void testConstructor() throws Exception {
        FileSystem fs = Jimfs.newFileSystem();
        Path path = fs.getPath("test.p12");
        PKCS12KeyStoreConfig cfg = new PKCS12KeyStoreConfig(path, "pw".toCharArray());
        CryptoStoreManager mgr = new CryptoStoreManager(cfg);
        assertNotNull(mgr.getKeyStore());
        mgr.saveKeystore();
        assertTrue(Files.exists(path));
    }
}
