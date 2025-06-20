module acmeserver.utils {
    exports de.morihofi.acmeserver.utils.base64;
    exports de.morihofi.acmeserver.utils.network.dns;
    exports de.morihofi.acmeserver.utils.regex;
    exports de.morihofi.acmeserver.utils.javaversion;

    requires acmeserver.types;
    requires okhttp3;
    requires org.dnsjava;
    requires org.slf4j;
    requires static lombok;
    requires com.github.spotbugs.annotations;
    requires org.bouncycastle.pkix;
    requires org.bouncycastle.provider;
    requires com.google.gson;
}