package de.morihofi.certgine.types.json;

import com.google.gson.Gson;
import de.morihofi.certgine.types.cryptography.revoke.RevokedCertificate;
import de.morihofi.certgine.types.database.entities.acme.AcmeOrder;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GsonFactoryTest {

    @Test
    void testInstantSerialization() {
        Gson gson = GsonFactory.createGson();
        Instant instant = Instant.parse("2021-03-15T10:15:30Z");
        assertEquals("\"2021-03-15T10:15:30Z\"", gson.toJson(instant));
    }

    @Test
    void testInstantDeserialization() {
        Gson gson = GsonFactory.createGson();
        Instant instant = gson.fromJson("\"2021-03-15T10:15:30Z\"", Instant.class);
        assertEquals(Instant.parse("2021-03-15T10:15:30Z"), instant);
    }

    @Test
    void testRevokedCertificateSerialization() {
        Gson gson = GsonFactory.createGson();
        RevokedCertificate cert = new RevokedCertificate(BigInteger.ONE,
                Instant.parse("2021-03-15T10:15:30Z"), 0);
        String json = gson.toJson(cert);
        assertTrue(json.contains("\"revocationDate\":\"2021-03-15T10:15:30Z\""));
    }

    @Test
    void testRevokedCertificateDeserialization() {
        Gson gson = GsonFactory.createGson();
        String json = "{\"serialNumber\":1,\"revocationDate\":\"2021-03-15T10:15:30Z\",\"revocationReason\":0}";
        RevokedCertificate cert = gson.fromJson(json, RevokedCertificate.class);
        assertEquals(Instant.parse("2021-03-15T10:15:30Z"), cert.revocationDate());
    }

    @Test
    void testAcmeOrderSerializationAndDeserialization() {
        Gson gson = GsonFactory.createGson();
        AcmeOrder order = new AcmeOrder();
        order.setCreated(Instant.parse("2021-03-15T10:15:30Z"));
        order.setNotAfter(Instant.parse("2021-03-16T10:15:30Z"));
        String json = gson.toJson(order);
        assertTrue(json.contains("\"created\":\"2021-03-15T10:15:30Z\""));
        assertTrue(json.contains("\"notAfter\":\"2021-03-16T10:15:30Z\""));

        AcmeOrder deserialized = gson.fromJson(json, AcmeOrder.class);
        assertEquals(Instant.parse("2021-03-15T10:15:30Z"), deserialized.getCreated());
        assertEquals(Instant.parse("2021-03-16T10:15:30Z"), deserialized.getNotAfter());
    }
}
