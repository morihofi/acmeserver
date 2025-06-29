/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.config;


import lombok.Data;

import java.io.Serializable;

/**
 * Configuration class for SSL server settings.
 *
 * <p>This class encapsulates the SSL server configuration settings such as allowing legacy resumption. The configuration is typically
 * loaded from an external source and managed using the provided getter and setter methods.</p>
 */
@Data
public class SslServerConfig implements Serializable {

    /**
     * Indicates whether legacy resumption is allowed. Legacy resumption is a feature that allows SSL/TLS sessions to be resumed even if the
     * server's session cache is no longer available. Used for compatibility with older web browsers
     */
    private boolean allowLegacyResumption = false;

    /**
     * Indicates whether SNI (Server Name Indication) checking is enabled for HTTPS hostname.
     */
    private boolean enableSniCheck = true;

}
