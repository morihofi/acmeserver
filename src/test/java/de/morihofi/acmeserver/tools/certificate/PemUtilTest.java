package de.morihofi.acmeserver.tools.certificate;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

import static org.junit.jupiter.api.Assertions.*;

class PemUtilTest {
    @BeforeEach
    public void setUp() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void testSaveAndLoadKeyPairToPEM() throws IOException, NoSuchAlgorithmException {
        // Generate a KeyPair for testing
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // Define paths for saving the keys
        Path privateKeyPath = Paths.get("private_key.pem");
        Path publicKeyPath = Paths.get("public_key.pem");

        // Save the KeyPair to PEM files
        PemUtil.saveKeyPairToPEM(keyPair, publicKeyPath, privateKeyPath);

        // Load the KeyPair from PEM files
        KeyPair loadedKeyPair = PemUtil.loadKeyPair(privateKeyPath, publicKeyPath);

        // Verify that the loaded KeyPair matches the original KeyPair
        assertEquals(keyPair.getPrivate(), loadedKeyPair.getPrivate());
        assertEquals(keyPair.getPublic(), loadedKeyPair.getPublic());

        // Clean up: delete the generated files
        Files.deleteIfExists(privateKeyPath);
        Files.deleteIfExists(publicKeyPath);
    }

    @Test
    public void testReadPublicKeyFromPem() throws IOException, NoSuchProviderException, InvalidKeySpecException, NoSuchAlgorithmException {
        // Generate a KeyPair for testing
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // Convert the public key to PEM format
        String pemPublicKey = PemUtil.convertToPem(keyPair.getPublic());

        // Read the public key from PEM format
        PublicKey loadedPublicKey = PemUtil.readPublicKeyFromPem(pemPublicKey);

        // Verify that the loaded public key matches the original public key
        assertEquals(keyPair.getPublic(), loadedPublicKey);
    }


    @Test
    public void testConvertPemToByteArray() throws IOException {
        // Define a PEM-encoded string for testing
        String pemString = """
                -----BEGIN CERTIFICATE-----
                MIIE8zCCAtugAwIBAgIUHsoyATT8Yx/ytU8C2axApd75PXEwDQYJKoZIhvcNAQEL
                BQAwITEfMB0GA1UEAwwWTXkgUm9vdCBDQSBJIGNhbiB0cnVzdDAeFw0yMzExMjMw
                OTA3NDBaFw0zNDA1MjYwODA3NDBaMCExHzAdBgNVBAMMFk15IFJvb3QgQ0EgSSBj
                YW4gdHJ1c3QwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQCk2zfguS5v
                flsFFXnA20vqbV2kfyQUVNpnKTOJOkT7XccDACkenzbH66luq4n5aeAk2PawFdIn
                rzhc0QG1klWVjt9Eh54Qpsv2jGLszERfvR81jppeB5uHQJnOERNuBUvrG/uS+2xy
                QlWb4qXoDT1xabtn4qORPJGA0+7hvl59FjEENx1Q7LVxi7d9Eb7reJQdmO67BI/o
                zzjbsw/QWuzUIgfEu83AtT44/kcei3QtG9ck6gTQJFKcBztR+x2I0q+zQrB3ynjm
                akwsx8Qs2R1h/krpmmtulfFGfaXK47TS98RluLmPod3pqIIakS0chEah68tzyYUw
                hrOEVPxxiAn5gwTj0Cj11XXc52mLqA9V9BBW3yl7WczAeZlfd84aUjW0uhlOyGpD
                pQOZ2kSIo0ZavY1hE9HvO+FhyEb8i0W+CzmbTYyZIf333JDT9Pvyjf+f9pbrUDYm
                g27Zt5/9y4BcWNktiKgwLAkUeloCXhvIdZ2PNE8Rg+LpieVduCVdfs8Hnh5WqRoF
                tpNvoKSCthyg49IMehz2bYzUNnO2ESFyVYXhFBb0MJL/gsvxi0vxjbkqH2xVR53J
                FuT/512buDzJZBo/GlqlEexvr86/np73BzOGeXFbFyHeaSrQ7dEAPWbJRk3/TUb/
                4zAR9qH7XZgvXe0OUL1tYbeqB11pjoCXKQIDAQABoyMwITAPBgNVHRMBAf8EBTAD
                AQH/MA4GA1UdDwEB/wQEAwIB/jANBgkqhkiG9w0BAQsFAAOCAgEAkaX0IvRZs6NF
                ggTzVvkM3xwGKHgNv/E50f5qT1JsxPePtCVQFjrOIEpA9XwKPgRHXgptB/C1MDyJ
                qQhCfKwjBrbIOMcgi10+nXh+dhM+vIrV59KL1FU7QiZwaFiNn++tgjo9iT0zb83t
                EryuYQD2Z4UTtSv57/c0XYyZCv+/RkMCVHQ1rhKWRILLMQRFYFToPX7B3RiaXQoz
                FYCEDQYsdK4AfamOpyLBppJ+cffMoNETJHZObzRLZfooKVCsmYSbsVeh/wBKDA8H
                NuvDbrZKEsR7bCdbrIwFxz4YVwIN6zCTSHrAASs8mdGpD+iM+npMPp1VSOw64Dc2
                CjhIX6QqfSmYwVBczewATB3cL/zx2cRYWmM/tRHcxtvMCYJ7tN4VazGgwgBDSAeQ
                Y1i7aqg7vsuJlV7A+JcR7+5erBeR5hHraOu/OvRFoNjd6gJDO72lgL0B0lvw2MkK
                rRakmNp2d0bahmyrK5QlnytMAhVkVsqOd82Cec1WUahbR4+y+dbKUlJtUgJD8hab
                Bv/6kfbIdB1OYfDpati3j5XKCVsNRYZkLHJiYIE7PXQekN8jYFNie8MAZbwGKzcD
                JcTahSsdUuTvX1mbEpRIRJGwAhuIrF0qANhiODzPHYDVOUvIHVNbGKVou3DRDZlK
                fXiI5mnBRnczShbC1/CldvozMxY3Yg8=
                -----END CERTIFICATE-----
                                
                """;

        // Convert the PEM-encoded string to a byte array
        byte[] byteArray = PemUtil.convertPemToByteArray(pemString);

        // Verify that the byte array is not empty
        assertNotNull(byteArray);
    }

}