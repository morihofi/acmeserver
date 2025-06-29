/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.config;

import lombok.Data;

import java.io.Serializable;

/**
 * Represents configuration parameters for a database connection, including engine, host, user, password, and database name.
 */
@Data
public class DatabaseConfig implements Serializable {
    private String user;
    private String password;
    private String jdbcUrl;
}
