/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.api.endpoints;

import com.google.gson.JsonObject;
import de.morihofi.certgine.server.common.intf.*;
import de.morihofi.certgine.types.database.entities.acme.AcmeProvisioner;
import de.morihofi.certgine.types.database.entities.acme.ProvisionerMeta;
import de.morihofi.certgine.types.httpserver.HandlerType;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.json.GsonFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DirectoryEndpointTest {

    static class DummyRequest implements Request {
        private final String path;
        private final String method;

        DummyRequest(String path, String method) {
            this.path = path;
            this.method = method;
        }

        @Override
        public HttpServletRequest getHttpServletRequest() {
            return null;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public String getMethod() {
            return method;
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public String getBody() {
            return "";
        }

        @Override
        public String getIP() {
            return "127.0.0.1";
        }

        @Override
        public String getQueryParam(String name) {
            return null;
        }

        @Override
        public byte[] getBodyBytes() {
            return new byte[0];
        }
    }

    static class DummyResponse extends Response {
        private final Map<String, String> headers = new HashMap<>();
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();

        @Override
        public void setHeader(String name, String value) {
            headers.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            return headers.get(name);
        }

        @Override
        public void setBodyBytes(byte[] data) {
            try {
                body.write(data);
            } catch (IOException ignored) {
            }
        }

        @Override
        public Map<String, String> getHeaders() {
            return headers;
        }

        @Override
        public OutputStream getOutputStream() {
            return body;
        }

        String getBodyString() {
            return body.toString(StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("returns directory JSON with correct links and content type")
    void testDirectoryJsonAndContentType() {
        IServerInstance server = Mockito.mock(IServerInstance.class);
        Mockito.when(server.getServerURL()).thenReturn("https://example.com");

        AcmeProvisioner provisioner = new AcmeProvisioner();
        provisioner.setName("testprov");
        provisioner.setMeta(new ProvisionerMeta("https://site", "https://tos"));

        try (MockedStatic<AcmeProvisioner> mocked = Mockito.mockStatic(AcmeProvisioner.class)) {
            mocked.when(() -> AcmeProvisioner.getForName(Mockito.any(), Mockito.eq("testprov")))
                    .thenReturn(provisioner);

            DirectoryEndpoint endpoint = new DirectoryEndpoint(server);

            Router router = new Router();
            router.addHandler(new Endpoint(HandlerType.GET, "/acme/{provisioner}/directory", ctx -> {}));
            DummyRequest req = new DummyRequest("/acme/testprov/directory", "GET");
            DummyResponse resp = new DummyResponse();
            HandlerContext ctx = new HandlerContext(req, resp, router);

            endpoint.handle(ctx);

            assertEquals("application/json", resp.getHeader("Content-Type"));
            JsonObject json = GsonFactory.createGson().fromJson(resp.getBodyString(), JsonObject.class);
            JsonObject meta = json.getAsJsonObject("meta");
            assertEquals("https://site", meta.get("website").getAsString());
            assertEquals("https://tos", meta.get("termsOfService").getAsString());
            String base = "https://example.com/acme/testprov";
            assertEquals(base + "/acme/new-acct", json.get("newAccount").getAsString());
            assertEquals(base + "/acme/new-nonce", json.get("newNonce").getAsString());
            assertEquals(base + "/acme/new-order", json.get("newOrder").getAsString());
            assertEquals(base + "/acme/revoke-cert", json.get("revokeCert").getAsString());
            assertEquals(base + "/acme/key-change", json.get("keyChange").getAsString());
        }
    }
}
