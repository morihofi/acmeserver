package de.morihofi.acmeserver.tools.certificate.cryptoops;

import de.morihofi.acmeserver.tools.javaversion.JavaVersion;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Iterator;
import java.util.concurrent.CancellationException;

public class PKCS11KeyStoreLoader {
    public static void main(String[] args) throws Exception {
        String libraryLocation = "C:\\SoftHSM2\\lib\\softhsm2-x64.dll";
        int slot = 0;
        String pin = "1234";

        KeyStore keyStore = loadPKCS11Keystore(pin, slot, libraryLocation);

        System.out.println(keyStore.size());
        for (Iterator<String> it = keyStore.aliases().asIterator(); it.hasNext(); ) {
            String alias = it.next();

            System.out.println(alias);
        }
    }

    /**
     * Loads a PKCS#11 KeyStore using the specified parameters.
     * This method initializes a PKCS#11 KeyStore using a specific slot and library location. It involves setting up a password handler for the KeyStore, which involves handling password callbacks specifically for the provided PIN. The method handles various exceptions and ensures that the KeyStore is properly loaded with the specified parameters.
     *
     * @param pin             The PIN code for accessing the KeyStore.
     * @param slot            The slot number of the PKCS#11 token.
     * @param libraryLocation The file system location of the PKCS#11 library.
     * @return The loaded KeyStore instance.
     * @throws CertificateException         If any of the certificates in the KeyStore could not be loaded.
     * @throws IOException                  If an I/O error occurs.
     * @throws NoSuchAlgorithmException     If the algorithm used to check the integrity of the KeyStore cannot be found.
     * @throws ClassNotFoundException       If the PKCS#11 provider class cannot be found.
     * @throws InvocationTargetException    If the underlying method throws an exception.
     * @throws InstantiationException       If the PKCS#11 provider class cannot be instantiated.
     * @throws IllegalAccessException       If the default constructor of the PKCS#11 provider class is not accessible.
     * @throws NoSuchMethodException        If a required method is not found.
     * @throws KeyStoreException            If the KeyStore cannot be initialized.
     * @throws CancellationException        If the KeyStore password is null.
     * @throws UnsupportedCallbackException If any of the callbacks provided for handling authentication are not supported.
     */
    public static KeyStore loadPKCS11Keystore(String pin, int slot, String libraryLocation) throws CertificateException, IOException, NoSuchAlgorithmException, ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, KeyStoreException {

        Provider selectedProvider = getPkcs11Provider(slot, libraryLocation);

        KeyStore keyStore = KeyStore.getInstance("PKCS11", selectedProvider);


        // register password handler
        AuthProvider authProvider = (AuthProvider) selectedProvider;
        authProvider.setCallbackHandler(new CallbackHandler() {
            @Override
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {

                for (Callback callback : callbacks) {
                    if (callback instanceof PasswordCallback) {
                        handlePasswordCallback((PasswordCallback) callback);
                    } else {
                        throw new UnsupportedCallbackException(callback, "Callback not supported " + callback.getClass().getName());
                    }
                }
            }

            private void handlePasswordCallback(PasswordCallback passCb) throws UnsupportedCallbackException {
                if (pin == null) {
                    throw new CancellationException("KeyStore Password is null");
                }

                passCb.setPassword(pin.toCharArray());
            }

        });

        keyStore.load(null, null);
        return keyStore;

    }

    /**
     * Retrieves a PKCS#11 Provider based on the specified slot and library location.
     * This private method is used to instantiate and configure a PKCS#11 provider based on the provided slot and library location. It handles different scenarios based on the Java version, either using reflection for older versions or the newer Provider configuration methods introduced in Java 9.
     *
     * @param slot            The slot number of the PKCS#11 token.
     * @param libraryLocation The file system location of the PKCS#11 library.
     * @return The configured Provider instance for the specified PKCS#11 token.
     * @throws InvocationTargetException If the underlying method throws an exception.
     * @throws InstantiationException    If the PKCS#11 provider class cannot be instantiated.
     * @throws IllegalAccessException    If the default constructor of the PKCS#11 provider class is not accessible.
     * @throws NoSuchMethodException     If a required method is not found.
     * @throws ClassNotFoundException    If the PKCS#11 provider class cannot be found.
     */
    private static Provider getPkcs11Provider(int slot, String libraryLocation) throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
        String pkcs11ConfigSettings = "name = Slot" + slot + "\n" + "library = " + libraryLocation + "\n" + "slotListIndex = " + slot;
        ByteArrayInputStream confStream = new ByteArrayInputStream(pkcs11ConfigSettings.getBytes());

        // instantiate the provider
        Provider p11Provider = null;
        if (JavaVersion.getJreVersion().isAtLeast(JavaVersion.JRE_VERSION_9)) {
            p11Provider = Security.getProvider("SunPKCS11");
            // add marker ("--") for inline config
            pkcs11ConfigSettings = "--" + pkcs11ConfigSettings;
            Method method = Provider.class.getMethod("configure", String.class);
            p11Provider = (Provider) method.invoke(p11Provider, pkcs11ConfigSettings);
        } else {
            Class<?> cl = Class.forName("sun.security.pkcs11.SunPKCS11");
            Constructor<?> cons = cl.getConstructor(InputStream.class);
            p11Provider = (Provider) cons.newInstance(confStream);
        }

        Security.addProvider(p11Provider);

        return p11Provider;
    }
}
