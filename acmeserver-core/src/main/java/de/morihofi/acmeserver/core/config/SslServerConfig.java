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

package de.morihofi.acmeserver.core.config;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration class for SSL server settings.
 *
 * <p>This class encapsulates the SSL server configuration settings such as allowing legacy resumption. The configuration is typically
 * loaded from an external source and managed using the provided getter and setter methods.</p>
 */
@Data
public class SslServerConfig {

    /**
     * Indicates whether legacy resumption is allowed. Legacy resumption is a feature that allows SSL/TLS sessions to be resumed even if the
     * server's session cache is no longer available. Used for compatibility with older web browsers
     */
    private boolean allowLegacyResumption = false;


}
