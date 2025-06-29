/*
 * SPDX-FileCopyrightText: 2004-2013 Wayne Grant
 * SPDX-FileCopyrightText: 2013-2023 Kai Kramer
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.morihofi.certgine.utils.javaversion;

import java.io.Serial;

/**
 * Represents a version exception.
 */
public class VersionException extends RuntimeException {
    /**
     * Internal Java version of class for serialisation
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new VersionException.
     */
    public VersionException() {
        super();
    }

    /**
     * Creates a new VersionException with the specified message.
     *
     * @param message Exception message
     */
    public VersionException(String message) {
        super(message);
    }

    /**
     * Creates a new VersionException with the specified message and cause throwable.
     *
     * @param causeThrowable The throwable that caused this exception to be thrown
     * @param message        Exception message
     */
    public VersionException(String message, Throwable causeThrowable) {
        super(message, causeThrowable);
    }

    /**
     * Creates a new VersionException with the specified cause throwable.
     *
     * @param causeThrowable The throwable that caused this exception to be thrown
     */
    public VersionException(Throwable causeThrowable) {
        super(causeThrowable);
    }
}
