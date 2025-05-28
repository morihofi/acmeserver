package de.morihofi.acmeserver.core.tools.meta;

import de.morihofi.acmeserver.core.Main;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Consumer;

@Slf4j
@Builder
@Getter
public class BuildMetadata {

    private String buildVersion;
    private String buildTime;
    private String gitCommit;
    private String gitClosestTagName;

    /**
     * Loads build and Git metadata from resource files and populates corresponding variables.
     */

    public static @NonNull BuildMetadata getInstance() {
        BuildMetadata.BuildMetadataBuilder builder = BuildMetadata.builder();

        log.info("Reading build metadata");
        loadMetadata("/build.properties", properties -> {
            builder.buildVersion(properties.getProperty("build.version"));
            builder.buildTime(properties.getProperty("build.date") + " UTC");
        });
        log.info("Reading git metadata");
        loadMetadata("/git.properties", properties -> {
            log.info("Loading git metadata");
            builder.gitCommit(properties.getProperty("git.commit.id.full"));
            builder.gitClosestTagName(properties.getProperty("git.closest.tag.name"));
        });

        return builder.build();
    }

    /**
     * Loads metadata from a specified file and processes it using a given consumer.
     *
     * @param fileName           the name of the file to load.
     * @param propertiesConsumer the consumer to process the loaded properties.
     */
    @Contract(pure = true)
    private static void loadMetadata(@NonNull String fileName, @NonNull Consumer<Properties> propertiesConsumer) {
        try (InputStream is = Main.class.getResourceAsStream(fileName)) {
            if (is != null) {
                Properties properties = new Properties();
                properties.load(is);
                propertiesConsumer.accept(properties);
            } else {
                log.warn("Unable to load metadata from {}", fileName);
            }
        } catch (IOException e) {
            log.error("Unable to load metadata from {}", fileName, e);
        }
    }
}
