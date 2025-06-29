/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme;

import de.morihofi.certgine.server.common.intf.AbstractStaticServlet;
import de.morihofi.certgine.server.common.intf.ServletMount;

@ServletMount(servletMountPoint = "/gethttpsforfree/*", protect = true)
public class GetHttpsForFreeServlet extends AbstractStaticServlet {
    @Override
    protected String getBasePath() {
        return "/gethttpsforfree"; // Path to the webapp directory in current classpath containing the static files
    }
}
