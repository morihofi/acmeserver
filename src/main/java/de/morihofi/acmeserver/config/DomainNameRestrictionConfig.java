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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents configuration for domain name restrictions, including a list of required suffixes and an enabled flag.
 */
@SuppressFBWarnings("EI_EXPOSE_REP")
public class DomainNameRestrictionConfig implements Serializable {
    @ConfigurationField(name = "Must end with domain names")
    private List<String> mustEndWith = new ArrayList<>();
    @ConfigurationField(name = "Enable domain name restriction", required = true)
    private boolean enabled = false;

    /**
     * Get the list of required suffixes that domain names must end with.
     *
     * @return The list of required suffixes.
     */
    public List<String> getMustEndWith() {
        return this.mustEndWith;
    }

    /**
     * Set the list of required suffixes that domain names must end with.
     *
     * @param mustEndWith The list of required suffixes to set.
     */
    public void setMustEndWith(List<String> mustEndWith) {
        this.mustEndWith = mustEndWith;
    }

    /**
     * Check if domain name restrictions are enabled.
     *
     * @return True if enabled, false otherwise.
     */
    public boolean getEnabled() {
        return this.enabled;
    }

    /**
     * Set the enabled status of domain name restrictions.
     *
     * @param enabled The enabled status to set.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
