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

/**
 * Represents the parameters for the Elliptic Curve Digital Signature Algorithm (ECDSA). This class extends {@link AlgorithmParams} to
 * include parameters specific to ECDSA, particularly the name of the elliptic curve used.
 */
public class EcdsaAlgorithmParams extends AlgorithmParams {
    private String curveName;

    /**
     * Retrieves the name of the elliptic curve used in the ECDSA algorithm. The curve name is a string that identifies the specific
     * elliptic curve being used, such as 'secp256k1' or 'secp384r1'.
     *
     * @return The name of the elliptic curve as a {@code String}.
     */
    public String getCurveName() {
        return curveName;
    }

    /**
     * Sets the name of the elliptic curve to be used in the ECDSA algorithm. This method allows specifying a particular elliptic curve by
     * name.
     *
     * @param curveName The name of the elliptic curve as a {@code String}.
     */
    public void setCurveName(String curveName) {
        this.curveName = curveName;
    }
}
