/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.exception;

public class ServerStartupException extends RuntimeException {
    public ServerStartupException(String message) {
        super(message);
    }
}
