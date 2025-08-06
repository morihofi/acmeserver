package de.morihofi.certgine.types.cryptography;

public class CryptoStoreManagerConstants {
    /**
     * Alias for the ACME API certificate in the keystore.
     */
    public static final String KEYSTORE_ALIASPREFIX_SERVER = "server_";

    /**
     * Prefix for aliases of intermediate certificate authorities in the keystore.
     */
    public static final String KEYSTORE_ALIASPREFIX_INTERMEDIATECA = "intermediateCA_";

    /**
     * Prefix for aliases of timestamp authority certificates in the keystore.
     */
    public static final String KEYSTORE_ALIASPREFIX_TSA = "tsa_";

}
