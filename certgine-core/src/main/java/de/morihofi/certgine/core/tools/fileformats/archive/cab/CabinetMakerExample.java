/*
 * SPDX-FileCopyrightText: 2013 Graham Rivers-Brown
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.tools.fileformats.archive.cab;


import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This class provides an example of how to create a CAB file using the {@link CabFile} class.
 */
@Slf4j
public class CabinetMakerExample {

    /**
     * Main method demonstrating the creation of a CAB file.
     *
     * @param args Command line arguments
     * @throws Exception if an error occurs during the file operations
     */
    public static void main(String[] args) throws IOException {
        CabFile cabFile = new CabFile.Builder()
                .addFile("pom.xml", Files.readAllBytes(Path.of("pom.xml")))
                .build();

        try (FileOutputStream fs = new FileOutputStream(new File("out.cab"))) {
            fs.write(cabFile.getCabFile());
        } catch (Exception e) {
            log.error("Error writing CAB file", e);
            throw new IOException("Failed to write CAB file", e);
        }
    }
}
