/*
 * SPDX-FileCopyrightText: 2013 Graham Rivers-Brown
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.tools.fileformats.archive.cab;



import lombok.Getter;
import lombok.Setter;

import java.util.Vector;

/**
 * Represents a folder entry in a CAB (Cabinet) archive. This class provides the necessary structure and methods to handle CAB folder entries,
 * including their metadata such as compression type, offset, and the number of CFDATA blocks.
 * <p>
 * This class is used in conjunction with other CAB file structures to create or manipulate CAB archives.
 * </p>
 * <p>
 * This code is originally based on the work of Graham Rivers-Brown and has been adapted for use in the Certgine project.
 * </p>
 */
@Setter
@Getter
public class CFFolder {

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
