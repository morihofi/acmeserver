package de.morihofi.acmeserver.core.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.morihofi.acmeserver.types.plugin.PluginProperties;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple JSON based storage for plugin configuration properties.
 */
@Slf4j
public class PluginConfigStore {
    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public PluginConfigStore(@NonNull Path file) {
        this.file = file;
    }

    /**
     * Loads properties from the configured file.
     *
     * @return loaded properties or an empty instance if the file is missing
     * @throws PluginConfigException if the file cannot be read
     */
    @NonNull
    public PluginProperties load() throws PluginConfigException {
        if (!Files.exists(file)) {
            return new PluginProperties();
        }
        try (Reader r = Files.newBufferedReader(file)) {
            PluginProperties props = gson.fromJson(r, PluginProperties.class);
            return props == null ? new PluginProperties() : props;
        } catch (IOException e) {
            throw new PluginConfigException("Failed reading plugin properties " + file, e);
        }
    }

    /** Saves the given properties to disk. */
    public void save(@NonNull PluginProperties props) throws PluginConfigException {
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                gson.toJson(props, w);
            }
        } catch (IOException e) {
            throw new PluginConfigException("Failed writing plugin properties " + file, e);
        }
    }
}
