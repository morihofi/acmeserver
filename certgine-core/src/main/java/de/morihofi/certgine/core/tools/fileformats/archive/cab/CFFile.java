/*
 * SPDX-FileCopyrightText: 2013 Graham Rivers-Brown
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */
package de.morihofi.certgine.core.tools.fileformats.archive.cab;

import java.util.Calendar;
import java.util.Vector;

/**
 * Represents a file entry in a CAB (Cabinet) archive
 * @author Graham Rivers-Brown
 */
public class CFFile {

    /**
     * Uncompressed size of this file in bytes.
     */
    private final int cbFile;

    /**
     * Uncompressed offset of this file in the folder.
     */
    private final int uoffFolderStart;

    /**
     * Index into the CFFOLDER area.
     */
    private final int iFolder;

    /**
     * Date stamp for this file.
     */
    private final int date;

    /**
     * Time stamp for this file.
     */
    private final int time;

    /**
     * Attribute flags for this file.
     */
    private final int attribs;

    /**
     * Name of this file.
     */
    private int[] szName;

    /**
     * Constructs a new CFFile instance with the specified parameters.
     *
     * @param filename     The name of the file.
     * @param size         The uncompressed size of the file in bytes.
     * @param offset       The uncompressed offset of the file in the folder.
     * @param folderNumber The index into the CFFOLDER area.
     */
    public CFFile(String filename, int size, int offset, int folderNumber) {
        Calendar c = Calendar.getInstance();

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        int hours = c.get(Calendar.HOUR_OF_DAY);
        int minutes = c.get(Calendar.MINUTE);
        int seconds = c.get(Calendar.SECOND);

        date = ((year - 1980) << 9) + (month << 5) + (day);
        time = (hours << 11) + (minutes << 5) + (seconds / 2);

        attribs = 0x80; // UTF encoding for filename string

        setFilename(filename);
        cbFile = size;
        uoffFolderStart = offset;
        iFolder = folderNumber;
    }
    /**
     * Sets the filename for this CFFile instance.
     *
     * @param filename The name of the file.
     */
    private void setFilename(String filename) {
        szName = new int[filename.length() + 1];
        for (int i = 0; i < filename.length(); i++) {
            szName[i] = filename.charAt(i);
        }
        szName[filename.length()] = 0x00;
    }

    /**
     * Converts the CFFile instance to a byte array representation.
     *
     * @return A {@code Vector<Byte>} containing the byte array representation of this CFFile.
     */
    public Vector<Byte> makeByteArray() {
        Vector<Byte> b = new Vector<>();

        b.addAll(convertToByte(cbFile, 4));
        b.addAll(convertToByte(uoffFolderStart, 4));
        b.addAll(convertToByte(iFolder, 2));
        b.addAll(convertToByte(date, 2));
        b.addAll(convertToByte(time, 2));
        b.addAll(convertToByte(attribs, 2));
        for (int j : szName) {
            b.addAll(convertToByte(j, 1));
        }

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
