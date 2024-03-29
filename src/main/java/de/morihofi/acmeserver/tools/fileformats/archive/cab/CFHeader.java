/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2013 Graham Rivers-Brown
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.morihofi.acmeserver.tools.fileformats.archive.cab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.Vector;

/**
 *
 * @author Graham Rivers-Brown
 */
public class CFHeader {
    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());
    private int[] signature;       /* cabinet file signature */
    private int reserved1;         /* reserved */
    private int cbCabinet;         /* size of this cabinet file in bytes */
    private int reserved2;         /* reserved */
    private int coffFiles;         /* offset of the first CFFILE entry */
    private int reserved3;         /* reserved */
    private int versionMinor;      /* cabinet file format version, minor */
    private int versionMajor;      /* cabinet file format version, major */
    private int cFolders;          /* number of CFFOLDER entries in this cabinet */
    private int cFiles;            /* number of CFFILE entries in this cabinet */
    private int flags;             /* cabinet file option indicators */
    private int setID;             /* must be the same for all cabinets in a set */
    private int iCabinet;          /* number of this cabinet file in a set */
    /*private int cbCFHeader;        /* (optional) size of per-cabinet reserved area */
    /*private int cbCFFolder;        /* (optional) size of per-folder reserved area */
    /*private int cbCFData;          /* (optional) size of per-datablock reserved area */
    /*private int[] abReserve;       /* (optional) per-cabinet reserved area */
    /*private int[] szCabinetPrev;   /* (optional) name of previous cabinet file */
    /*private int[] szDiskPrev;      /* (optional) name of previous disk */
    /*private int[] szCabinetNext;   /* (optional) name of next cabinet file */
    /*private int[] szDiskNext;      /* (optional) name of next disk */

    
    
    public CFHeader()
    {
        //fill in known values
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
    
    public void setCbCabinet(int cbCabinet)
    {
        this.cbCabinet = cbCabinet;
    }
    
    public void setCoffFiles(int coffFiles)
    {
        this.coffFiles = coffFiles;
    }
    
    public void setCFolders(int cFolders)
    {
        this.cFolders = cFolders;
    }
    
    public void setCFiles(int cFiles)
    {
        this.cFiles = cFiles;
    }
    
    public Vector<Byte> makeByteArray()
    {
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
    
    private Vector<Byte> convertToByte(int val, int numBytes)
    {
        Vector<Byte> b = new Vector<>();
        Integer tempInt;
        Byte byteToAdd;
        if (numBytes == 1)
        {
            tempInt = val;
            byteToAdd = tempInt.byteValue();
            b.add(byteToAdd);
        }
        else if (numBytes == 2)
        {
            tempInt = 0xFF & val;
            byteToAdd = tempInt.byteValue();
            b.add(byteToAdd);
            tempInt = (0xFF00 & val) >>> 8;
            byteToAdd = tempInt.byteValue();
            b.add(byteToAdd);
        }
        else if (numBytes == 3)
        {
            tempInt = 0xFF & val;
            byteToAdd = tempInt.byteValue();
            b.add(byteToAdd);
            tempInt = (0xFF00 & val) >>> 8;
            byteToAdd = tempInt.byteValue();
            b.add(byteToAdd);
            tempInt = (0xFF0000 & val) >>> 16;
            byteToAdd = tempInt.byteValue();
            b.add(byteToAdd);
        }
        else if (numBytes == 4)
        {
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
        }
        else
        {
            b.add(Byte.valueOf("255"));
        }
        
        return b;
    }
}
