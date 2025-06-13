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

    /** Loads properties from the configured file. */
    @NonNull
    public PluginProperties load() {
        if (!Files.exists(file)) {
            return new PluginProperties();
        }
        try (Reader r = Files.newBufferedReader(file)) {
            PluginProperties props = gson.fromJson(r, PluginProperties.class);
            return props == null ? new PluginProperties() : props;
        } catch (IOException e) {
            log.warn("Failed reading plugin properties {}", file, e);
            return new PluginProperties();
        }
    }

    /** Saves the given properties to disk. */
    public void save(@NonNull PluginProperties props) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                gson.toJson(props, w);
            }
        } catch (IOException e) {
            log.warn("Failed writing plugin properties {}", file, e);
        }
    }
}
