/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.tools.fileformats.archive.cab;


import lombok.Getter;
import lombok.Setter;

import java.util.Vector;

/**
 * Represents the header of a CAB (Cabinet) archive. This class provides the necessary structure and methods to handle CAB header entries,
 * including their metadata such as signature, reserved fields, file offsets, version, and more.
 * <p>
 * This class is used in conjunction with other CAB file structures to create or manipulate CAB archives.
 * </p>
 * <p>
 * This code is originally based on the work of Graham Rivers-Brown and has been adapted for use in the ACME server project.
 * </p>
 */
@Setter
@Getter
public class CFHeader {

    /**
     * Cabinet file option indicators.
     */
    private final int flags;

    /**
     * Cabinet file signature.
     */
    private final int[] signature;

    /**
     * Reserved field.
     */
    private final int reserved1;

    /**
     * Size of this cabinet file in bytes.
     */
    private int cbCabinet;

    /**
     * Reserved field.
     */
    private final int reserved2;

    /**
     * Offset of the first CFFILE entry.
     */
    private int coffFiles;

    /**
     * Reserved field.
     */
    private final int reserved3;

    /**
     * Cabinet file format version, minor.
     */
    private final int versionMinor;

    /**
     * Cabinet file format version, major.
     */
    private final int versionMajor;

    /**
     * Number of CFFOLDER entries in this cabinet.
     */
    private int cFolders;

    /**
     * Number of CFFILE entries in this cabinet.
     */
    private int cFiles;

    /**
     * Must be the same for all cabinets in a set.
     */
    private final int setID;

    /**
     * Number of this cabinet file in a set.
     */
    private final int iCabinet;

    public CFHeader() {
        // fill in known values
        signature = new int[4];
        signature[0] = 'M';
        signature[1] = 'S';
        signature[2] = 'C';
        signature[3] = 'F';
        reserved1 = 0;
        reserved2 = 0;
        reserved3 = 0;
        versionMinor = 3;
        versionMajor = 1;
        flags = 0;
        setID = 0;
        iCabinet = 0;
    }


    /**
     * Converts the CFHeader instance to a byte array representation.
     *
     * @return A {@code Vector<Byte>} containing the byte array representation of this CFHeader.
     */
    public Vector<Byte> makeByteArray() {
        Vector<Byte> b = new Vector<>();

        b.addAll(convertToByte(signature[0], 1));
        b.addAll(convertToByte(signature[1], 1));
        b.addAll(convertToByte(signature[2], 1));
        b.addAll(convertToByte(signature[3], 1));
        b.addAll(convertToByte(reserved1, 4));
        b.addAll(convertToByte(cbCabinet, 4));
        b.addAll(convertToByte(reserved2, 4));
        b.addAll(convertToByte(coffFiles, 4));
        b.addAll(convertToByte(reserved3, 4));
        b.addAll(convertToByte(versionMinor, 1));
        b.addAll(convertToByte(versionMajor, 1));
        b.addAll(convertToByte(cFolders, 2));
        b.addAll(convertToByte(cFiles, 2));
        b.addAll(convertToByte(flags, 2));
        b.addAll(convertToByte(setID, 2));
        b.addAll(convertToByte(iCabinet, 2));

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
