package de.morihofi.certgine.types.cryptography.revoke;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RevocationReasonTest {

    @Test
    void fromCodeReturnsEnum() {
        assertEquals(RevocationReason.KEY_COMPROMISE, RevocationReason.fromCode(1));
    }

    @Test
    void fromCodeInvalidThrows() {
        assertThrows(IllegalArgumentException.class, () -> RevocationReason.fromCode(7));
    }
}
