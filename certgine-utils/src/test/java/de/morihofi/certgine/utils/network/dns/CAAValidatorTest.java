/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.network.dns;

import org.junit.jupiter.api.Test;
import org.xbill.DNS.CAARecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CAAValidatorTest {

    @Test
    void testNoRecordsAllowed() throws Exception {
        assertTrue(CAAValidator.evaluateCaaRecords(List.of(), "ca.example"));
    }

    @Test
    void testRecordAllows() throws Exception {
        Record rec = new CAARecord(Name.fromString("example.com."), 1, 3600, 0, "issue", "ca.example");
        assertTrue(CAAValidator.evaluateCaaRecords(List.of(rec), "ca.example"));
    }

    @Test
    void testRecordForbids() throws Exception {
        Record rec = new CAARecord(Name.fromString("example.com."), 1, 3600, 0, "issue", "other.ca");
        assertFalse(CAAValidator.evaluateCaaRecords(List.of(rec), "ca.example"));
    }
}
