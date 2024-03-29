/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2013 Graham Rivers-Brown
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.morihofi.acmeserver.tools.fileformats.archive.cab;

import java.util.ArrayList;
import java.util.Vector;

public class CabFile {

    private CFHeader header;
    private Vector<CFFolder> folders;
    private Vector<CFFile> files;
    private Vector<CFData> cfdata;
    private Vector<Byte> data;


    public CabFile(Vector<String> filenames, Vector<byte[]> fileContents) {
        header = new CFHeader(); //initialise header
        cfdata = new Vector<>();
        Vector<Byte> alldata = new Vector<>();
        for (byte[] b : fileContents) {
            for (byte value : b) {
                alldata.add(value);
            }
        }
        //alldata is a vector of bytes containing all bytes to be saved
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

        //make CFFiles
        CFFile tempFile;
        int offset = 0;
        files = new Vector<>();
        for (int i = 0; i < filenames.size(); i++) {
            tempFile = new CFFile(filenames.get(i), fileContents.get(i).length, offset, 0);
            offset = offset + fileContents.get(i).length;
            files.add(tempFile);
        }

        //make folders
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

        //Vector<Byte> temp = cfdata.makeByteArray();
        //for (Byte b : temp)
        //    System.out.print((char)b.intValue());

        for (CFData d : cfdata) {
            data.addAll(d.makeByteArray());
        }
    }

    public byte[] getCabFile() {
        byte[] bytes = new byte[data.size()];
        for (int i = 0; i < data.size(); i++) {
            bytes[i] = data.get(i);
        }
        return bytes;
    }

    // Builder class
    public static class Builder {
        private Vector<String> filenames = new Vector<>();
        private Vector<byte[]> fileContents = new Vector<>();

        public Builder addFile(String filename, byte[] content) {
            this.filenames.add(filename);
            this.fileContents.add(content);
            return this;
        }

        public CabFile build() {

            /*Vector<String> initFileNames = new Vector<>();
            Vector<byte[]> initFileData = new Vector<>();
*/
           /* for (int i = 0; i < Math.min(fileContents.size(), filenames.size()); i++) {


                byte[] file1 = ;

                initFileNames.add(FILENAME_1);

                initFileData.add(file1);
            }*/


            return new CabFile(filenames,fileContents);
        }
    }

}
