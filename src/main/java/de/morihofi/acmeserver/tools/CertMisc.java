package de.morihofi.acmeserver.tools;

import java.math.BigInteger;

public class CertMisc {
    public static BigInteger generateSerialNumber() {
        return BigInteger.valueOf(System.currentTimeMillis());
    }
}
