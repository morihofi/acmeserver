/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.events;

import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event requesting the web server to reload its TLS configuration.
 */
@AllArgsConstructor
@Getter
public class AcmeTlsCertificateHotReloadEvent extends AbstractEvent{
    private final IServerInstance serverInstance;
}
