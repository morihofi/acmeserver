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

package de.morihofi.acmeserver.exception.objects;

/**
 * A class representing an error response in an API or web service. It typically contains information about the error type and additional
 * details.
 */
public class ErrorResponse {
    private String type;
    private String detail;

    /**
     * Get the type of the error.
     *
     * @return The error type, which can be a short name or code for the error.
     */
    public String getType() {
        return type;
    }

    /**
     * Set the type of the error.
     *
     * @param type The error type to set.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get additional details about the error.
     *
     * @return Additional error details, which may provide more context or information about the error.
     */
    public String getDetail() {
        return detail;
    }

    /**
     * Set additional details about the error.
     *
     * @param detail Additional error details to set.
     */
    public void setDetail(String detail) {
        this.detail = detail;
    }
}
