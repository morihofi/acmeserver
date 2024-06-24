/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2013 Graham Rivers-Brown
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.morihofi.acmeserver.tools.fileformats.archive.cab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.Vector;

/**
 * Represents a folder entry in a CAB (Cabinet) archive. This class provides the necessary structure and methods to handle CAB folder entries,
 * including their metadata such as compression type, offset, and the number of CFDATA blocks.
 * <p>
 * This class is used in conjunction with other CAB file structures to create or manipulate CAB archives.
 * </p>
 * <p>
 * This code is originally based on the work of Graham Rivers-Brown and has been adapted for use in the ACME server project.
 * </p>
 */
public class CFFolder {
    /**
     * Logger for logging information and debugging.
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    /**
     * No compression type indicator.
     */
    public static final int NO_COMPRESSION = 0;

    /**
     * Compression type indicator.
     */
    private final int typeCompress;

    /**
     * Offset of the first CFDATA block in this folder.
     */
    private int coffCabStart;

    /**
     * Number of CFDATA blocks in this folder.
     */
    private int cCFData;

    /**
     * Constructs a new CFFolder instance with no compression type.
     */
    public CFFolder() {
        typeCompress = CFFolder.NO_COMPRESSION;
    }

    /**
     * Sets the offset of the first CFDATA block in this folder.
     *
     * @param coffCabStart The offset to set.
     */
    public void setCoffCabStart(int coffCabStart) {
        this.coffCabStart = coffCabStart;
    }

    /**
     * Sets the number of CFDATA blocks in this folder.
     *
     * @param cCFData The number of CFDATA blocks to set.
     */
    public void setCCFData(int cCFData) {
        this.cCFData = cCFData;
    }

    /**
     * Converts the CFFolder instance to a byte array representation.
     *
     * @return A {@code Vector<Byte>} containing the byte array representation of this CFFolder.
     */
    public Vector<Byte> makeByteArray() {
        Vector<Byte> b = new Vector<>();

        b.addAll(convertToByte(coffCabStart, 4));
        b.addAll(convertToByte(cCFData, 2));
        b.addAll(convertToByte(typeCompress, 2));

        return b;
    }

    /**
     * Converts an integer value to a byte array representation with the specified number of bytes.
     *
     * @param val      The integer value to convert.
     * @param numBytes The number of bytes to use for the conversion.
     * @return A {@code Vector<Byte>} containing the byte array representation of the integer value.
     */
    private Vector<Byte> convertToByte(int val, int numBytes) {
        Vector<Byte> b = new Vector<>();
        int tempInt;
        byte byteToAdd;
        if (numBytes == 1) {
            tempInt = val;
            byteToAdd = (byte) tempInt;
            b.add(byteToAdd);
        } else if (numBytes == 2) {
            tempInt = 0xFF & val;
            byteToAdd = (byte) tempInt;
            b.add(byteToAdd);
            tempInt = (0xFF00 & val) >>> 8;
            byteToAdd = (byte) tempInt;
            b.add(byteToAdd);
        } else if (numBytes == 3) {
            tempInt = 0xFF & val;
            byteToAdd = (byte) tempInt;
            b.add(byteToAdd);
            tempInt = (0xFF00 & val) >>> 8;
            byteToAdd = (byte) tempInt;
            b.add(byteToAdd);
            tempInt = (0xFF0000 & val) >>> 16;
            byteToAdd = (byte) tempInt;
            b.add(byteToAdd);
        } else if (numBytes == 4) {
            tempInt = 0xFF & val;
            byteToAdd = (byte) tempInt;
            b.add(byteToAdd);
            tempInt = (0xFF00 & val) >>> 8;
            byteToAdd = (byte) tempInt;
            b.add(byteToAdd);
            tempInt = (0xFF0000 & val) >>> 16;
            byteToAdd = (byte) tempInt;
            b.add(byteToAdd);
            tempInt = (0xFF000000 & val) >>> 24;
            byteToAdd = (byte) tempInt;
            b.add(byteToAdd);
        } else {
            b.add(Byte.valueOf("255"));
        }

        return b;
    }
}
