/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

open module certgine.types {
    exports de.morihofi.certgine.types.intf.network.dns;
    exports de.morihofi.certgine.types.config.network;
    exports de.morihofi.certgine.types.intf.network;
    exports de.morihofi.certgine.types.runtime;
    exports de.morihofi.certgine.types.intf;
    exports de.morihofi.certgine.types.json;
    exports de.morihofi.certgine.types.cryptography.keystore;
    exports de.morihofi.certgine.types.cryptography.revoke;
    exports de.morihofi.certgine.types.exception.exceptions;
    exports de.morihofi.certgine.types.events;
    exports de.morihofi.certgine.types.config;
    exports de.morihofi.certgine.types.server;
    exports de.morihofi.certgine.types.database.entities.authority;
    exports de.morihofi.certgine.types.database.entities.user;
    exports de.morihofi.certgine.types.modules;
    exports de.morihofi.certgine.types.cryptography;
    exports de.morihofi.certgine.types.dns;

    requires com.github.spotbugs.annotations;
    requires com.google.gson;
    requires jakarta.persistence;
    requires jakarta.transaction;
    requires jakarta.cdi;
    requires java.sql;
    requires org.slf4j;
    requires static lombok;
    requires org.hibernate.orm.core;
    requires okhttp3;
    requires org.dnsjava;
    requires jakarta.servlet;
    requires java.naming;
}