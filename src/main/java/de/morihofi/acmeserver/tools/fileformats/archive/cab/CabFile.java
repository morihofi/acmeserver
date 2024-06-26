/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 * Copyright (c) 2013 Graham Rivers-Brown
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.tools.fileformats.archive.cab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Represents a CAB file that can be constructed from multiple files and their contents.
 */
public class CabFile {

    /**
     * Logger instance for logging events related to CAB file processing.
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Header information for the CAB file.
     */
    private final CFHeader header;

    /**
     * List of folders in the CAB file.
     */
    private final Vector<CFFolder> folders;

    /**
     * List of files in the CAB file.
     */
    private final Vector<CFFile> files;

    /**
     * List of data blocks in the CAB file.
     */
    private final Vector<CFData> cfdata;

    /**
     * Complete data content of the CAB file.
     */
    private final Vector<Byte> data;

    /**
     * Constructs a new CAB file with the given filenames and file contents.
     *
     * @param filenames    Vector of filenames to be included in the CAB file.
     * @param fileContents Vector of file contents corresponding to the filenames.
     */
    public CabFile(Vector<String> filenames, Vector<byte[]> fileContents) {
        header = new CFHeader(); // Initialize header
        cfdata = new Vector<>();
        Vector<Byte> alldata = new Vector<>();
        for (byte[] b : fileContents) {
            for (byte value : b) {
                alldata.add(value);
            }
        }
        // alldata is a vector of bytes containing all bytes to be saved
        ArrayList<Vector<Byte>> dataSet = new ArrayList<>();

        while (!alldata.isEmpty()) {
            Vector<Byte> tempBytes = new Vector<>();
            for (int i = 0; i < 0x8000 && i < alldata.size(); i++) {
                tempBytes.add(alldata.get(i));
            }
            dataSet.add(tempBytes);
            if (alldata.size() >= 0x8000) {
                alldata.subList(0, 0x8000).clear();
            } else {
                alldata.clear();
            }
        }
        for (Vector<Byte> b : dataSet) {
            cfdata.add(new CFData(b));
        }

        // Create CFFiles
        CFFile tempFile;
        int offset = 0;
        files = new Vector<>();
        for (int i = 0; i < filenames.size(); i++) {
            tempFile = new CFFile(filenames.get(i), fileContents.get(i).length, offset, 0);
            offset = offset + fileContents.get(i).length;
            files.add(tempFile);
        }

        // Create folders
        folders = new Vector<>();
        CFFolder tempFolder = new CFFolder();
        tempFolder.setCCFData(cfdata.size());
        folders.add(tempFolder);
        int dataBlockOffset = header.makeByteArray().size();
        int filesBlockOffset;
        for (CFFolder f : folders) {
            dataBlockOffset = dataBlockOffset + f.makeByteArray().size();
        }
        filesBlockOffset = dataBlockOffset;
        for (CFFile f : files) {
            dataBlockOffset = dataBlockOffset + f.makeByteArray().size();
        }
        folders.get(0).setCoffCabStart(dataBlockOffset);
        header.setCFolders(folders.size());
        header.setCFiles(files.size());
        header.setCoffFiles(filesBlockOffset);
        int cabFileSize = 0;
        for (CFData d : cfdata) {
            cabFileSize = cabFileSize + d.makeByteArray().size();
        }
        header.setCbCabinet(dataBlockOffset + cabFileSize);

        data = new Vector<>();
        data.addAll(header.makeByteArray());
        for (CFFolder f : folders) {
            data.addAll(f.makeByteArray());
        }
        for (CFFile f : files) {
            data.addAll(f.makeByteArray());
        }

        for (CFData d : cfdata) {
            data.addAll(d.makeByteArray());
        }
    }

    /**
     * Retrieves the complete CAB file as a byte array.
     *
     * @return Byte array representing the CAB file.
     */
    public byte[] getCabFile() {
        byte[] bytes = new byte[data.size()];
        for (int i = 0; i < data.size(); i++) {
            bytes[i] = data.get(i);
        }
        return bytes;
    }

    /**
     * Builder class for constructing a CabFile instance.
     */
    public static class Builder {
        /**
         * Filenames. Index same as file contents
         */
        private final Vector<String> filenames = new Vector<>();
        /**
         * Contents of the files. Index same as file names
         */
        private final Vector<byte[]> fileContents = new Vector<>();

        /**
         * Adds a file to the CAB file.
         *
         * @param filename The name of the file.
         * @param content  The content of the file.
         * @return The Builder instance.
         */
        public Builder addFile(String filename, byte[] content) {
            this.filenames.add(filename);
            this.fileContents.add(content);
            return this;
        }

        /**
         * Builds the CabFile instance.
         *
         * @return The constructed CabFile instance.
         */
        public CabFile build() {
            return new CabFile(filenames, fileContents);
        }
    }
}
