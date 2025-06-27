open module certgine.cryptography {
    requires org.bouncycastle.pkix;
    requires org.bouncycastle.provider;
    requires org.bouncycastle.tls;
    requires org.jose4j;
    requires certgine.utils;
    requires certgine.types;
    requires org.slf4j;
    requires static lombok;
    requires org.hibernate.orm.core;
    requires java.sql;
    requires com.github.spotbugs.annotations;
    exports de.morihofi.certgine.cryptography.acme;
    exports de.morihofi.certgine.cryptography.certificate;
    exports de.morihofi.certgine.cryptography.csr;
    exports de.morihofi.certgine.cryptography.keys;
    exports de.morihofi.certgine.cryptography.keystore;
    exports de.morihofi.certgine.cryptography.ocsp;
    exports de.morihofi.certgine.cryptography.pem;
    exports de.morihofi.certgine.cryptography.randomness;
    exports de.morihofi.certgine.cryptography.crl;
}
