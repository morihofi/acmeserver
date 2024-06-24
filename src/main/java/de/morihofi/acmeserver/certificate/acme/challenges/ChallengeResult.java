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

package de.morihofi.acmeserver.certificate.acme.challenges;

/**
 * Represents the result of a challenge verification.
 */
public class ChallengeResult {
    /**
     * Indicates whether the challenge was successful.
     */
    private boolean successful;

    /**
     * Reason for the error if the challenge was not successful.
     */
    private String errorReason;

    /**
     * Constructs a new ChallengeResult with the specified success status and error reason.
     *
     * @param successful  {@code true} if the challenge was successful, otherwise {@code false}.
     * @param errorReason The reason for the error if the challenge was not successful.
     */
    public ChallengeResult(boolean successful, String errorReason) {
        this.successful = successful;
        this.errorReason = errorReason;
    }

    /**
     * Checks if the challenge was successful.
     *
     * @return {@code true} if the challenge was successful, otherwise {@code false}.
     */
    public boolean isSuccessful() {
        return successful;
    }

    /**
     * Sets the success status of the challenge.
     *
     * @param successful {@code true} if the challenge was successful, otherwise {@code false}.
     */
    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    /**
     * Retrieves the reason for the error if the challenge was not successful.
     *
     * @return The error reason.
     */
    public String getErrorReason() {
        return errorReason;
    }

    /**
     * Sets the reason for the error if the challenge was not successful.
     *
     * @param errorReason The error reason to set.
     */
    public void setErrorReason(String errorReason) {
        this.errorReason = errorReason;
    }
}
