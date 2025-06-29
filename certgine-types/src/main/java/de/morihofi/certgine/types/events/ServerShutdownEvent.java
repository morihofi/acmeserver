/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.events;

import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Published when the runtime shutdown hook is executed. Use this to cleanly
 * close resources before the JVM exits.
 */
@AllArgsConstructor
@Getter
public class ServerShutdownEvent extends AbstractEvent {
    private final IServerInstance serverInstance;
}
