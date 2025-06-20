open module acmeserver.types {
    exports de.morihofi.acmeserver.types.intf.network.dns;
    exports de.morihofi.acmeserver.types.config.network;
    exports de.morihofi.acmeserver.types.intf.network;
    exports de.morihofi.acmeserver.types.runtime;
    exports de.morihofi.acmeserver.types.database.entities;
    exports de.morihofi.acmeserver.types.intf;
    exports de.morihofi.acmeserver.types.api.acme.dns;
    exports de.morihofi.acmeserver.types.cryptography.keystore;
    exports de.morihofi.acmeserver.types.cryptography.revoke;
    exports de.morihofi.acmeserver.types.database.enums;
    exports de.morihofi.acmeserver.types.exception.exceptions;
    exports de.morihofi.acmeserver.types.events;
    exports de.morihofi.acmeserver.types.config;
    exports de.morihofi.acmeserver.types.server;

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
}