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

package de.morihofi.acmeserver.config.certificateAlgorithms;

import de.morihofi.acmeserver.configPreprocessor.annotation.ConfigurationField;

import java.io.Serializable;

/**
 * Abstract base class for algorithm parameters. This class provides a common structure for various types of algorithm parameters, allowing
 * them to be serialized and managed in a unified way. Subclasses should provide specific implementations and additional properties relevant
 * to the particular algorithm.
 */
public abstract class AlgorithmParams implements Serializable {

    /**
     * The type of the algorithm (e.g., "rsa" or "ecdsa").
     */
    @ConfigurationField(name = "Parameter type (rsa or ecdsa)", required = true)
    private String type;

    /**
     * Retrieves the type of the algorithm. The type is a string identifier that describes the specific algorithm or set of parameters
     * represented by the subclass.
     *
     * @return The type of the algorithm as a {@code String}.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the algorithm. This method allows specifying the type, providing a way to identify the specific algorithm or
     * parameters represented by the subclass.
     *
     * @param type The type of the algorithm as a {@code String}.
     */
    public void setType(String type) {
        this.type = type;
    }
}
