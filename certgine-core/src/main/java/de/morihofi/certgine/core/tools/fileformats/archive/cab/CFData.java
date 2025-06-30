/*
 * SPDX-FileCopyrightText: 2013 Graham Rivers-Brown
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.tools.fileformats.archive.cab;


import java.util.Vector;

/**
 * Represents a CFData block in a CAB file, which contains compressed data.
 */
public class CFData {
    /**
     * Checksum of this CFDATA entry.
     */
    private final int csum;

    /**
     * Number of compressed bytes in this block.
     */
    private final int cbData;

    /**
     * Number of uncompressed bytes in this block.
     */
    private final int cbUncomp;

    /**
     * Compressed data bytes.
     */
    private final Vector<Byte> ab;

    /**
     * Constructs a CFData block with the given data.
     *
     * @param data The data to be stored in the CFData block.
     */
    public CFData(Vector<Byte> data) {
        ab = data;
        cbData = ab.size();
        cbUncomp = ab.size();
        csum = 0; // No checksum for now
    }

    /**
     * Converts the CFData block into a byte array.
     *
     * @return A Vector of Byte representing the CFData block.
     */
    public Vector<Byte> makeByteArray() {
        Vector<Byte> b = new Vector<>();

        b.addAll(convertToByte(csum, 4));
        b.addAll(convertToByte(cbData, 2));
        b.addAll(convertToByte(cbUncomp, 2));
        b.addAll(ab);

        return b;
    }

    /**
     * Converts an integer value to a Vector of Byte.
     *
     * @param val The integer value to convert.
     * @param numBytes The number of bytes to use for the conversion.
     * @return A Vector of Byte representing the integer value.
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
