/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to
  the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.tools.certificate.dataExtractor;

import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.Security;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsrDataUtilTest {

    @BeforeAll
    static void prepare() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    @Test
    @DisplayName("CSR: Get Identifiers only from CN")
    void extractDnsFromCN() throws IOException {
        final String csrString =
                "MIIE_zCCAucCAQAwgZAxCzAJBgNVBAYTAkRFMRMwEQYDVQQIDApCYW5hbmFsYW5kMRMwEQYDVQQHDApUb21hdG90b3duMRkwFwYDVQQKDBBFdmlsIENvcnBvcmF0aW9uMRYwFAYDVQQDDA1ldmlsY29ycC5ldmlsMSQwIgYJKoZIhvcNAQkBFhVjb250YWN0QGV2aWxjb3JwLmV2aWwwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQCyLoIwu5wPguQQMi6ru7xPQSLXIU_ymAyyXS9N69EO4qkM561uyi5o-tqauhYisSaIPHa-xxHEeLwBueYNCOTurCwvXfTguv42emAQT0FzDPaw0ofXHFTkZe7kAIyAP6WAeatuyM96Bkzb379YN_1wv-Fyg8CBGHj-OVu_uAdXdVRJ8gccGp-FeYL9jJ1W5z8LHqBO8F6Flpuej8YXMUijTWlA4oihc6a3lh9EKQxRvSV3cAclNjNOMN4j0s4GV2WpW5sd5iP4tVCAwiIowEng_Ry2xv84xjxqjXxhBsrmxebF4w_eqfXP_ny-wmn4HhhGehEFj20lx7Wf-wXozbtFgyC4vJbnDNtYjuuSd_hhjWhplIyf4y54H1Uywa23gl2c_QWx7Mc2L0Y-m3y_xIpeu7CX0KF8HXYvGUsrxExRpm6lD3SD7Ec9LGwGbIByGSarF3yWYMYSosjAam73Z2B28wiE_Tf8qvkLqehtNwbEPJ3J2LUzxvExmqtKWThIaYKNBb5X31e7v7WZjY01Ag2U0_HhoCUkBf2F2LkLsw59ypHu5LgVfy7GDGOwO09aAeb5168l-7pnlD6AymuMfOq4g2zlCnpmiL_-ain_R5yAlaCk4S5SSZCpb39dsBRWUYqJzHakhybYTWLc2FtYKBgpFLthV_zpC6EmKcAil4p7YQIDAQABoCkwJwYJKoZIhvcNAQkOMRowGDAJBgNVHRMEAjAAMAsGA1UdDwQEAwIF4DANBgkqhkiG9w0BAQsFAAOCAgEAiPkmRMrBwQcnfLroNPYiMkpBYZAeVRKdjH2GTGSLuafhlbyZczogLvB6oNGUYbYWOKD2doLURqeHQR5u1mmQBmqcl1opPpyVnsX3N6KBA_SGO4Sc8AeJqGdFsmRDryezo0x4EwnJC9VQS3jCbq1PWxc64cjf0-CgDZlLMDO_ic-5OffIuxHYiWIgBWn27P8q-LyR5iMO61h7Nq7epXjufwFA0M9cI6quU6t_5B2JTXvDpQho9XDo5-SFVM__ppjmveYKaOi8fnX0eDZg_6iPR9Wb3eVNz_icO5EARnJkb-h2-HrxNB3Gsts9XO8P5XAc3pyr3UNiNDfzyeSUUNNuTb7nlmvTuqg1bjILBPl6F2zK739zqVXbELsjQfZSguQl0epKF0RaIa-GiArGkeZ0HGKid3BpWLlIfijxhzIbqN2z1_Ftl8vD5f19Y6ZVhxYDSxJq3U24u8peOgvJZXqlYe7m6ix_KU6kpDsv3vRF7Rn5BBF3rqup-G-rOBFCCVAuu6ItVazJGnMnvNQIXXA9TQHQz85mTdSmacr8JoBHxFuABxSl1M8XMODFmNEp2EGR5wIk_E109QpQL0EwobVYzXGltoFe9bbvu0mWCIKtNVASJb9NXe1m0dHb6g9VOKifWAKP_8MLyUSCwneK8aKYW-Jexf-02AWLjXP4OlAPw2Q=";
        Set<Identifier> identifiers = CsrDataUtil.getDomainsAndIPsFromCSR(csrString);

        assertEquals(1, identifiers.size()); // One Identifier is in the CSR

        Identifier identifier = identifiers.toArray(new Identifier[0])[0]; // Get the first and only entry

        assertEquals("DNS", identifier.getType());
        assertEquals(Identifier.IDENTIFIER_TYPE.DNS, identifier.getTypeAsEnumConstant());
        assertEquals("evilcorp.evil", identifier.getValue());
    }
}
