/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.servlets.handlerapi.endpoints;

import com.google.gson.JsonObject;
import de.morihofi.certgine.acme.AcmeModule;
import de.morihofi.certgine.acme.AcmeModuleInstance;
import de.morihofi.certgine.acme.types.entities.AcmeProvisioner;
import de.morihofi.certgine.acme.types.entities.AcmeProvisionerMeta;
import de.morihofi.certgine.server.common.intf.Endpoint;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.server.common.intf.Router;
import de.morihofi.certgine.server.common.intf.testing.MockRequest;
import de.morihofi.certgine.server.common.intf.testing.MockResponse;
import de.morihofi.certgine.types.httpserver.HandlerType;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.json.GsonFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DirectoryEndpointTest {

    @Test
    @DisplayName("returns directory JSON with correct links and content type")
    void testDirectoryJsonAndContentType() {
        IServerInstance server = Mockito.mock(IServerInstance.class);
        Mockito.when(server.getServerURL()).thenReturn("https://example.com");

        AcmeProvisioner provisioner = new AcmeProvisioner();
        provisioner.setName("testprov");
        provisioner.setMeta(new AcmeProvisionerMeta("https://site", "https://tos"));

        try (MockedStatic<AcmeProvisioner> mocked = Mockito.mockStatic(AcmeProvisioner.class)) {
            mocked.when(() -> AcmeProvisioner.getForName(Mockito.any(), Mockito.eq("testprov")))
                    .thenReturn(provisioner);

            DirectoryEndpoint endpoint = new DirectoryEndpoint(new AcmeModuleInstance(new AcmeModule(server)));

            Router router = new Router();
            router.addHandler(new Endpoint(HandlerType.GET, "/acme/{provisioner}/directory", ctx -> {
            }));
            MockRequest req = new MockRequest().path("/acme/testprov/directory").method("GET");
            MockResponse resp = new MockResponse();
            HandlerContext ctx = new HandlerContext(req, resp, router);

            endpoint.handle(ctx);

            assertEquals("application/json", resp.getHeader("Content-Type"));
            JsonObject json = GsonFactory.createGson().fromJson(resp.getBodyAsString(), JsonObject.class);
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
