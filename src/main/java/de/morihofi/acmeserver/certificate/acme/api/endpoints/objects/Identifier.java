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

package de.morihofi.acmeserver.certificate.acme.api.endpoints.objects;

import java.util.Locale;

/**
 * ACME Identifier used in Requests from ACME Clients
 */
public class Identifier {

    /**
     * Type of the DNS identifier, mostly <code>dns</code>. Can also be <code>ip</code>
     */
    private String type;

    /**
     * Value of the identifier, so it is the DNS Name
     */
    private String value;

    /**
     * Constructs a new Identifier with specified type and value. This constructor initializes the Identifier with a specific type (e.g.,
     * 'dns') and its corresponding value.
     *
     * @param type  The type of the identifier (e.g., 'dns').
     * @param value The value of the identifier (e.g., a DNS name).
     */
    public Identifier(String type, String value) {
        this.type = type;
        this.value = value;
    }

    /**
     * Default constructor for Identifier. Constructs an Identifier with default values for type and value.
     */
    public Identifier() {
    }

    /**
     * Retrieves the type of this identifier. The type usually indicates the category of the identifier (e.g., 'dns' for DNS names).
     *
     * @return The type of the identifier as a {@code String}.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of this identifier. This method allows changing the type of the identifier.
     *
     * @param type The new type of the identifier as a {@code String}.
     */
    public void setType(String type) {
        this.type = type;
    }

    public IDENTIFIER_TYPE getTypeAsEnumConstant() {
        return IDENTIFIER_TYPE.getTypeByName(type.toLowerCase(Locale.ROOT));
    }

    /**
     * Retrieves the value of this identifier. The value is specific to the type of the identifier (e.g., a DNS name for type 'dns').
     *
     * @return The value of the identifier as a {@code String}.
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of this identifier. This method allows changing the value of the identifier.
     *
     * @param value The new value of the identifier as a {@code String}.
     */
    public void setValue(String value) {
        this.value = value;
    }

    public enum IDENTIFIER_TYPE {
        DNS, IP;

        public static IDENTIFIER_TYPE getTypeByName(String name) {
            switch (name) {
                case "dns" -> {
                    return DNS;
                }
                case "ip" -> {
                    return IP;
                }
                default -> throw new IllegalArgumentException("Unknown or unsupported identifier type " + name);
            }
        }
    }
}
