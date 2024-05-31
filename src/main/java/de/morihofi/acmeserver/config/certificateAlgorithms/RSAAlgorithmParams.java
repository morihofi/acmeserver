/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.config.certificateAlgorithms;

import de.morihofi.acmeserver.configPreprocessor.annotation.ConfigurationClassExtends;
import de.morihofi.acmeserver.configPreprocessor.annotation.ConfigurationField;

/**
 * Represents the parameters for the RSA (Rivest–Shamir–Adleman) algorithm. This class extends {@link AlgorithmParams} to include parameters
 * specific to RSA, particularly the size of the key used in the algorithm.
 */
@ConfigurationClassExtends(configName = "rsa")
public class RSAAlgorithmParams extends AlgorithmParams {
    @ConfigurationField(name = "Key Size (Bit)", required = true)
    private int keySize;

    /**
     * Retrieves the key size used in the RSA algorithm. The key size is an integer that specifies the length of the RSA key in bits.
     *
     * @return The key size in bits as an {@code int}.
     */
    public int getKeySize() {
        return this.keySize;
    }

    /**
     * Sets the key size to be used in the RSA algorithm. This method allows specifying the length of the RSA key in bits.
     *
     * @param keySize The key size in bits as an {@code int}.
     */
    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }
}
