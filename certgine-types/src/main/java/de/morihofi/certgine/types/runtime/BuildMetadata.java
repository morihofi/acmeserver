/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.runtime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BuildMetadata {
    private String buildVersion;
    private String buildTime;
    private String gitCommit;
    private String gitClosestTagName;
}
