/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

module certgine.utils {
    exports de.morihofi.certgine.utils.base64;
    exports de.morihofi.certgine.utils.network.dns;
    exports de.morihofi.certgine.utils.regex;
    exports de.morihofi.certgine.utils.javaversion;

    requires certgine.types;
    requires okhttp3;
    requires org.dnsjava;
    requires org.slf4j;
    requires static lombok;
    requires com.github.spotbugs.annotations;
    requires org.bouncycastle.pkix;
    requires org.bouncycastle.provider;
    requires com.google.gson;
    requires com.cronutils;
}