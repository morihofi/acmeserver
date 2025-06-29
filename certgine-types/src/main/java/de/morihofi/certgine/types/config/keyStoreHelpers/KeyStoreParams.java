/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.config.keyStoreHelpers;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Abstract base class for KeyStore parameters. This class provides a common structure for different types of KeyStore parameters, allowing
 * them to be serialized and managed in a unified way. Subclasses should provide specific implementations and additional properties relevant
 * to the particular type of KeyStore.
 */
@Data
@Slf4j
public abstract class KeyStoreParams implements Serializable {
    protected String type;
    protected char[] password;

    /**
     * Clears the password stored in this KeyStoreParams instance.
     */
    public void clearPassword() {
        if (password != null) {
            Arrays.fill(password, '\0');
            password = null;
            log.info("In-Memory config keystore password cleared successfully.");
        }else {
            log.warn("Attempted to clear in-memory keystore password, but it was already null.");
        }
    }
}
