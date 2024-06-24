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
 * Represents a CFData block in a CAB file, which contains compressed data.
 */
public class CFData {
    /**
     * Logger for logging information and debugging.
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

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
        Integer tempInt;
        Byte byteToAdd;
        if (numBytes == 1) {
            tempInt = val;
            byteToAdd = tempInt.byteValue();
            b.add(byteToAdd);
        } else if (numBytes == 2) {
            tempInt = 0xFF & val;
            byteToAdd = tempInt.byteValue();
            b.add(byteToAdd);
            tempInt = (0xFF00 & val) >>> 8;
            byteToAdd = tempInt.byteValue();
            b.add(byteToAdd);
        } else if (numBytes == 3) {
            tempInt = 0xFF & val;
            byteToAdd = tempInt.byteValue();
            b.add(byteToAdd);
            tempInt = (0xFF00 & val) >>> 8;
            byteToAdd = tempInt.byteValue();
            b.add(byteToAdd);
            tempInt = (0xFF0000 & val) >>> 16;
            byteToAdd = tempInt.byteValue();
            b.add(byteToAdd);
        } else if (numBytes == 4) {
            tempInt = 0xFF & val;
            byteToAdd = tempInt.byteValue();
            b.add(byteToAdd);
            tempInt = (0xFF00 & val) >>> 8;
            byteToAdd = tempInt.byteValue();
            b.add(byteToAdd);
            tempInt = (0xFF0000 & val) >>> 16;
            byteToAdd = tempInt.byteValue();
            b.add(byteToAdd);
            tempInt = (0xFF000000 & val) >>> 24;
            byteToAdd = tempInt.byteValue();
            b.add(byteToAdd);
        } else {
            b.add(Byte.valueOf("255"));
        }

        return b;
    }
}
