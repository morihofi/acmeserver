package de.morihofi.certgine.core.web;

import com.google.common.jimfs.Jimfs;
import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.cryptography.keystore.CryptoStoreManager;
import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.types.database.entities.authority.RootCa;
import de.morihofi.certgine.types.database.entities.timestamp.TsaAuthority;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.intf.ICryptoStoreManager;
import de.morihofi.certgine.types.intf.INonceManager;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.intf.network.INetworkClient;
import de.morihofi.certgine.types.runtime.BuildMetadata;
import de.morihofi.certgine.types.cryptography.keystore.PKCS12KeyStoreConfig;
import de.morihofi.certgine.core.web.JettyCertificateHelper;
import jakarta.servlet.http.HttpServlet;
import lombok.NonNull;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class WebServerServletUnloadTest {

    @ServletMount(servletMountPoint = "/dummy")
    public static class DummyServlet extends HttpServlet {}

    static class DummyServer implements IServerInstance {
        private final CryptoStoreManager mgr;
        private final EventBus bus;
        DummyServer(CryptoStoreManager mgr, EventBus bus){this.mgr=mgr;this.bus=bus;}
        @NotNull
        @NonNull @Override public String getServerURL(){return "";}
        @NotNull
        @NonNull @Override public org.hibernate.Session getDatabaseSession(){return null;}
        @NotNull
        @NonNull @Override public ICryptoStoreManager getCryptoStoreManager(){return mgr;}
        @NotNull
        @NonNull @Override public Config getAppConfig(){return new Config();}
        @NotNull
        @NonNull @Override public INonceManager getNonceManager(){return null;}
        @NotNull
        @NonNull @Override public RootCa getRootCa(){return null;}
        @NotNull
        @NonNull @Override public TsaAuthority getTsaAuthority(){return null;}
        @NotNull
        @NonNull @Override public BuildMetadata getBuildMetadata(){return BuildMetadata.builder().build();}
        @NotNull
        @NonNull @Override public INetworkClient getNetworkClient(){return null;}
        @NotNull
        @NonNull @Override public EventBus getEventBus(){return bus;}
        @NotNull
        @NonNull @Override public java.util.Set<de.morihofi.certgine.types.server.StartupFlag> getStartupFlags(){return Collections.emptySet();}
    }

    @Test
    @DisplayName("unloadServlet removes unprotected servlet by class")
    void testUnloadServletByClass() throws Exception {
        FileSystem fs = Jimfs.newFileSystem();
        Path ksPath = fs.getPath("store.p12");
        CryptoStoreManager mgr = new CryptoStoreManager(new PKCS12KeyStoreConfig(ksPath, "pw".toCharArray()));
        EventBus bus = new EventBus();

        try (MockedStatic<JettyCertificateHelper> mock = Mockito.mockStatic(JettyCertificateHelper.class)) {
            mock.when(() -> JettyCertificateHelper.generateAcmeApiClientCertificate(Mockito.any())).thenReturn(null);
            IServerInstance si = new DummyServer(mgr, bus);
            WebServer ws = new WebServer(si);
            ServletContextHandler context = new ServletContextHandler();
            var addMethod = WebServer.class.getDeclaredMethod("addServlet", ServletContextHandler.class, Class.class);
            addMethod.setAccessible(true);
            addMethod.invoke(ws, context, DummyServlet.class);

            assertEquals(1, context.getServletHandler().getServletMappings().length);

            var unload = WebServer.class.getDeclaredMethod("unloadServlet", ServletContextHandler.class, Class.class);
            unload.setAccessible(true);
            unload.invoke(ws, context, DummyServlet.class);

            assertEquals(0, context.getServletHandler().getServletMappings().length);
        }
    }
}
