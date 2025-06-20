open module acmeserver.cryptography {
    requires org.bouncycastle.pkix;
    requires org.bouncycastle.provider;
    requires org.jose4j;
    requires acmeserver.utils;
    requires acmeserver.types;
    requires org.slf4j;
    requires static lombok;
    requires org.hibernate.orm.core;
    requires java.sql;
    requires com.github.spotbugs.annotations;
    exports de.morihofi.acmeserver.cryptography.acme;
    exports de.morihofi.acmeserver.cryptography.certificate;
    exports de.morihofi.acmeserver.cryptography.csr;
    exports de.morihofi.acmeserver.cryptography.keys;
    exports de.morihofi.acmeserver.cryptography.keystore;
    exports de.morihofi.acmeserver.cryptography.ocsp;
    exports de.morihofi.acmeserver.cryptography.pem;
    exports de.morihofi.acmeserver.cryptography.randomness;
    exports de.morihofi.acmeserver.cryptography.crl;
}
