module acmeserver {
    requires jdk.crypto.cryptoki;
    requires org.bouncycastle.provider;
    requires org.apache.logging.log4j;
    requires jakarta.mail;
    requires com.google.gson;
    requires io.javalin;
    requires org.bouncycastle.pkix;
    requires org.json;
    requires org.jose4j;
    requires org.dnsjava;
    requires okhttp3;
    requires org.hibernate.orm.core;
    requires org.reflections;
    requires com.github.spotbugs.annotations;
    requires java.sql;
    requires jakarta.persistence;
    requires jakarta.transaction;
    requires jakarta.cdi;
    requires jakarta.interceptor;

    //Gson
    opens de.morihofi.acmeserver.config to com.google.gson;
    opens de.morihofi.acmeserver.config.proxy to com.google.gson;
    opens de.morihofi.acmeserver.config.certificateAlgorithms to com.google.gson;

    //Hibernate
    opens de.morihofi.acmeserver.database.objects to org.hibernate.orm.core;

    opens webstatic;
}