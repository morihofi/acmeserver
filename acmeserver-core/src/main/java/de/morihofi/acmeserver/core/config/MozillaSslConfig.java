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

import java.io.Serializable;

/**
 * Represents the configuration for Mozilla SSL settings.
 * This class holds the settings for enabling Mozilla SSL configuration,
 * specifying the version of the SSL configuration guidelines, and the configuration name.
 */
@Data
public class MozillaSslConfig implements Serializable {

    /**
     * Indicates whether Mozilla SSL-Config settings are enabled.
     */
    private boolean enabled = false;

    /**
     * Specifies the version of the Mozilla SSL-Config guidelines to use.
     */
    private String version = "5.7";

    /**
     * Specifies the name of the Mozilla SSL-Config configuration to use.
     */
    private String configuration = "intermediate";

}
