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

package de.morihofi.acmeserver.certificate.objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

/**
 * Represents the contents of a KeyStore file. This class encapsulates a key pair, a certificate, and the alias used for the certificate
 * within the KeyStore.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public record KeyStoreFileContent(KeyPair keyPair, X509Certificate cert, String certificateAlias) {
    /**
     * Constructs a new KeyStoreFileContent object with the specified key pair, certificate, and certificate alias.
     *
     * @param keyPair          The {@link KeyPair} to be stored in the KeyStore.
     * @param cert             The {@link X509Certificate} to be associated with the key pair.
     * @param certificateAlias The alias used to reference the certificate in the KeyStore.
     */
    public KeyStoreFileContent {
    }

    /**
     * Retrieves the key pair stored in the KeyStore file.
     *
     * @return The {@link KeyPair} associated with the certificate in the KeyStore.
     */
    @Override
    public KeyPair keyPair() {
        return keyPair;
    }

    /**
     * Retrieves the certificate stored in the KeyStore file.
     *
     * @return The {@link X509Certificate} associated with the key pair in the KeyStore.
     */
    @Override
    public X509Certificate cert() {
        return cert;
    }

    /**
     * Retrieves the alias of the certificate stored in the KeyStore file. The alias is used as a reference to the certificate within the
     * KeyStore.
     *
     * @return The alias of the certificate as a {@code String}.
     */
    @Override
    public String certificateAlias() {
        return certificateAlias;
    }
}
