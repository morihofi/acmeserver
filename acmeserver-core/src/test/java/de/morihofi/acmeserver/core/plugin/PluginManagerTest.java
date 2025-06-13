package de.morihofi.acmeserver.core.plugin;

import de.morihofi.acmeserver.core.Main;
import de.morihofi.acmeserver.types.events.EventBus;
import de.morihofi.acmeserver.types.events.ServerStartedEvent;
import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import de.morihofi.acmeserver.types.intf.INonceManager;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.intf.network.INetworkClient;
import de.morihofi.acmeserver.types.runtime.BuildMetadata;
import de.morihofi.acmeserver.types.config.Config;
import de.morihofi.acmeserver.types.plugin.PluginProperties;
import de.morihofi.acmeserver.core.plugin.PluginConfigStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PluginManagerTest {

    static class DummyServerInstance implements IServerInstance {
        private final EventBus bus = new EventBus();
        @Override public String getServerURL() { return ""; }
        @Override public org.hibernate.Session getDatabaseSession() { return null; }
        @Override public ICryptoStoreManager getCryptoStoreManager() { return null; }
        @Override public Config getAppConfig() { return new Config(); }
        @Override public INonceManager getNonceManager() { return null; }
        @Override public de.morihofi.acmeserver.types.database.entities.RootCa getRootCa() { return null; }
        @Override public BuildMetadata getBuildMetadata() { return BuildMetadata.builder().build(); }
        @Override public INetworkClient getNetworkClient() { return null; }
        @Override public EventBus getEventBus() { return bus; }
    }

    private static void createPluginJar(Path jarPath, long version) throws IOException {
        Path srcDir = Files.createTempDirectory("plugin-src");
        Path pkgDir = srcDir.resolve("testplugin");
        Files.createDirectories(pkgDir);
        Path javaFile = pkgDir.resolve("TestPlugin.java");
        String src = "package testplugin;" +
                "import de.morihofi.acmeserver.types.intf.*;" +
                "import de.morihofi.acmeserver.types.events.*;" +
                "import de.morihofi.acmeserver.types.plugin.*;" +
                "public class TestPlugin implements IServerPlugin, EventSubscriber {" +
                " public static boolean triggered=false;" +
                " public void initialize(IServerInstance si, java.util.Map<String,PluginProperty> props){si.getEventBus().register(this);}" +
                " public java.util.List<Class<? extends AbstractEvent>> canHandle(){return java.util.List.of(ServerStartedEvent.class);}" +
                " public void onEvent(AbstractEvent e){if(e instanceof ServerStartedEvent) triggered=true;}" +
                " public String getPluginId(){return \"testplugin\";}" +
                " public long getPluginVersion(){return " + version + ";}" +
                " public void propertyUpdate(long pv, java.util.Map<String,PluginProperty> props){" +
                "  if(pv==0){props.put(\"fresh\", PluginProperty.ofBoolean(\"fresh\", true));}" +
                "  else{props.put(\"updated\", PluginProperty.ofBoolean(\"updated\", true));}}" +
                "}";
        Files.writeString(javaFile, src, StandardOpenOption.CREATE);

        Path classesDir = Files.createTempDirectory("plugin-classes");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            fm.setLocation(javax.tools.StandardLocation.CLASS_OUTPUT, List.of(classesDir.toFile()));
            fm.setLocation(javax.tools.StandardLocation.CLASS_PATH,
                    List.of(new File("../acmeserver-types/target/classes")));
            compiler.getTask(null, fm, null, null, null, fm.getJavaFileObjects(javaFile.toFile())).call();
        }

        try (JarOutputStream jarOut = new JarOutputStream(Files.newOutputStream(jarPath))) {
            Path classFile = classesDir.resolve("testplugin/TestPlugin.class");
            jarOut.putNextEntry(new JarEntry("testplugin/TestPlugin.class"));
            jarOut.write(Files.readAllBytes(classFile));
            jarOut.closeEntry();

            jarOut.putNextEntry(new JarEntry("META-INF/acmeserver-plugin"));
            jarOut.write("testplugin.TestPlugin\n".getBytes());
            jarOut.closeEntry();
        }
    }

    @Test
    @DisplayName("plugin registers subscriber via event bus")
    void testPluginLoad() throws Exception {
        Path pluginDir = Main.resolveDataPluginsDir().resolve("testplugin");
        Files.createDirectories(pluginDir);
        Path jar = pluginDir.resolve("testplugin.jar");
        createPluginJar(jar, 1);

        DummyServerInstance si = new DummyServerInstance();
        PluginManager pm = new PluginManager(si);
        pm.loadPlugins();

        si.getEventBus().publish(new ServerStartedEvent(si));

        Class<?> pluginClass = pm.getLoader().getNewInitializedClassInstance("testplugin.TestPlugin");
        boolean triggered = pluginClass.getField("triggered").getBoolean(null);
        assertTrue(triggered);

        Files.deleteIfExists(jar);
    }

    @Test
    @DisplayName("plugin properties persisted and updated on version change")
    void testPropertyUpdate() throws Exception {
        Path pluginDir = Main.resolveDataPluginsDir().resolve("testplugin");
        Files.createDirectories(pluginDir);
        Path jar = pluginDir.resolve("testplugin.jar");

        // initial load
        createPluginJar(jar, 1);
        DummyServerInstance si = new DummyServerInstance();
        PluginManager pm = new PluginManager(si);
        pm.loadPlugins();

        PluginConfigStore store = new PluginConfigStore(pluginDir.resolve("config.json"));
        PluginProperties props = store.load();
        assertEquals(1, props.getVersion());
        assertTrue(props.getProperties().containsKey("fresh"));

        // upgrade
        createPluginJar(jar, 2);
        pm = new PluginManager(si);
        pm.loadPlugins();
        props = store.load();
        assertEquals(2, props.getVersion());
        assertTrue(props.getProperties().containsKey("updated"));

        Files.deleteIfExists(jar);
        Files.deleteIfExists(pluginDir.resolve("config.json"));
    }
}
