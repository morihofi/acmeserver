/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.types.database.enums;

import lombok.Getter;

/**
 * Enumeration representing the possible statuses of an ACME order or challenge. <a href="https://datatracker.ietf.org/doc/html/rfc8555#section-7.1.6">See Section 7.1.6 of RFC8555</a>
 *
 * <p><b>For Challenges:</b></p>
 * <pre>
 *             pending
 *                |
 *                | Receive
 *                | response
 *                V
 *            processing <-+
 *                |   |    | Server retry or
 *                |   |    | client retry request
 *                |   +----+
 *                |
 *                |
 *    Successful  |   Failed
 *    validation  |   validation
 *      +---------+---------+
 *      |                   |
 *      V                   V
 *    valid              invalid
 * </pre>
 *
 * <p><b>For Authorizations:</b></p>
 * <pre>
 *                pending --------------------+
 *                   |                        |
 * Challenge failure |                        |
 *        or         |                        |
 *       Error       |  Challenge valid       |
 *         +---------+---------+              |
 *         |                   |              |
 *         V                   V              |
 *      invalid              valid            |
 *                             |              |
 *                             |              |
 *                             |              |
 *              +--------------+--------------+
 *              |              |              |
 *              |              |              |
 *       Server |       Client |   Time after |
 *       revoke |   deactivate |    "expires" |
 *              V              V              V
 *           revoked      deactivated      expired
 * </pre>
 * <p><b>State Transitions for Authorization Objects</b></p>
 * <pre>
 *    pending --------------+
 *       |                  |
 *       | All authz        |
 *       | "valid"          |
 *       V                  |
 *     ready ---------------+
 *       |                  |
 *       | Receive          |
 *       | finalize         |
 *       | request          |
 *       V                  |
 *   processing ------------+
 *       |                  |
 *       | Certificate      | Error or
 *       | issued           | Authorization failure
 *       V                  V
 *     valid             invalid
 * </pre>
 * <p><b>State Transitions for Order Objects</b></p>
 * <pre>
 *                     valid
 *                       |
 *                       |
 *           +-----------+-----------+
 *    Client |                Server |
 *   deactiv.|                revoke |
 *           V                       V
 *      deactivated               revoked
 * </pre>
 */
@Getter
public enum AcmeStatus {
    /**
     * Status indicating that the order or challenge is pending.
     */
    PENDING("pending"),

    /**
     * Status indicating that the order or challenge is ready.
     */
    READY("ready"),

    /**
     * Status indicating that the order or challenge is being processed.
     */
    PROCESSING("processing"),

    /**
     * Status indicating that the order or challenge is valid.
     */
    VALID("valid"),

    /**
     * Status indicating that the order or challenge is invalid.
     */
    INVALID("invalid"),

    /**
     * Status indicating that the order or challenge has been revoked.
     */
    REVOKED("revoked"),

    /**
     * Status indicating that the order or challenge has been deactivated.
     */
    DEACTIVATED("deactivated"),

    /**
     * Status indicating that the order or challenge has expired.
     */
    EXPIRED("expired");

    /**
     * The RFC-compliant name of the status.
     */
    private final String rfcName;

    /**
     * Constructs an {@code AcmeStatus} with the specified RFC-compliant name.
     *
     * @param rfcName The RFC-compliant name of the status.
     */
    AcmeStatus(String rfcName) {
        this.rfcName = rfcName;
    }

}
