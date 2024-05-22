/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.exception;

import de.morihofi.acmeserver.exception.objects.ErrorResponse;

/**
 * An abstract base class for custom exceptions related to the Automated Certificate Management Environment (ACME). ACME exceptions extend
 * {@link IllegalArgumentException}.
 */
public abstract class ACMEException extends IllegalArgumentException {

    /**
     * Constructs a new ACME exception with the specified error message.
     *
     * @param s The error message associated with the exception.
     */
    public ACMEException(String s) {
        super(s);
    }

    /**
     * Get the HTTP status code associated with this exception.
     *
     * @return The HTTP status code that should be sent in the response when this exception is thrown.
     */
    public abstract int getHttpStatusCode();

    /**
     * Get the ErrorResponse associated with this exception.
     *
     * @return An ErrorResponse object containing additional error information.
     */
    public abstract ErrorResponse getErrorResponse();
}
