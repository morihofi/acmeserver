/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.meta;

import de.morihofi.certgine.types.runtime.BuildMetadata;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Reads build and Git property resources to expose runtime metadata such as the
 * build version and source control information.
 *
 * <p>The loaded values are used to populate a {@link BuildMetadata} instance
 * that describes the build at runtime.</p>
 */
@Slf4j
@Getter
public class BuildMetadataImpl {

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
    private static void loadMetadata(@NonNull String fileName, @NonNull Consumer<Properties> propertiesConsumer) {
        try (InputStream is = MethodHandles.lookup().lookupClass().getResourceAsStream(fileName)) {
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
