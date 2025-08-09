/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.ui.frontend.legacy.helper;

import de.morihofi.certgine.utils.path.ResourcePathResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.DirectoryCodeResolver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TemplateHelper {
    public static TemplateEngine createTemplateEngine() {
        boolean isDev = !ResourcePathResolver.isRunningFromJar();

        if (isDev) {
            log.info("Looks like this application is running from an IDE or outside a jar, using a JRE compiler resolver");
            DirectoryCodeResolver codeResolver = new DirectoryCodeResolver(
                    ResourcePathResolver.resolveModulePath("certgine-mod-web-legacy/src/main/jte"));
            return TemplateEngine.create(codeResolver, ContentType.Html);
        } else {
            log.info("Running inside a JAR, using a precompiled template engine");

            return TemplateEngine.createPrecompiled(ContentType.Html);
        }
    }
}
