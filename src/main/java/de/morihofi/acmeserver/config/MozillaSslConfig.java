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

package de.morihofi.acmeserver.config;

import de.morihofi.acmeserver.configPreprocessor.annotation.ConfigurationField;

import java.io.Serializable;

/**
 * Represents the configuration for Mozilla SSL settings.
 * This class holds the settings for enabling Mozilla SSL configuration,
 * specifying the version of the SSL configuration guidelines, and the configuration name.
 */
public class MozillaSslConfig implements Serializable {

    /**
     * Indicates whether Mozilla SSL-Config settings are enabled.
     */
    @ConfigurationField(name = "Enable Mozilla SSL-Config settings")
    private boolean enabled = false;

    /**
     * Specifies the version of the Mozilla SSL-Config guidelines to use.
     */
    @ConfigurationField(name = "Mozilla SSL-Config Version")
    private String version = "5.7";

    /**
     * Specifies the name of the Mozilla SSL-Config configuration to use.
     */
    @ConfigurationField(name = "Mozilla SSL-Config Configuration name")
    private String configuration = "intermediate";

    /**
     * Checks if the Mozilla SSL-Config settings are enabled.
     *
     * @return true if enabled, false otherwise.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether the Mozilla SSL-Config settings should be enabled.
     *
     * @param enabled true to enable, false to disable.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the version of the Mozilla SSL-Config guidelines.
     *
     * @return the version of the guidelines.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version of the Mozilla SSL-Config guidelines.
     *
     * @param version the version to set.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Gets the name of the Mozilla SSL-Config configuration.
     *
     * @return the configuration name.
     */
    public String getConfiguration() {
        return configuration;
    }

    /**
     * Sets the name of the Mozilla SSL-Config configuration.
     *
     * @param configuration the configuration name to set.
     */
    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }
}
