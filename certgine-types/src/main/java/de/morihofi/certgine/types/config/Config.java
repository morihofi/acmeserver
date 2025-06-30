/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.config;

import com.google.gson.annotations.SerializedName;
import de.morihofi.certgine.types.config.keyStoreHelpers.KeyStoreParams;
import de.morihofi.certgine.types.config.network.NetworkConfig;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;

import java.io.Serializable;

/**
 * Represents a configuration for this Certgine instance.
 */
@Data
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class Config implements Serializable {
    @SerializedName("$schema")
    private String jsonSchema;

    private ServerConfig server;

    private KeyStoreParams keyStore;

    private DatabaseConfig database;

    private NetworkConfig network = new NetworkConfig();

}
