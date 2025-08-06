/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.exception.exceptions;

import de.morihofi.certgine.types.exception.ACMEException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for verifying HTTP status codes of ACME exceptions.
 */
class ACMEExceptionStatusCodeTest {

    static Stream<Arguments> exceptionProvider() {
        return Stream.of(
                Arguments.of(new ACMEAccountNotFoundException("msg"), 404),
                Arguments.of(new ACMEResourceNotFoundException("msg"), 404),
                Arguments.of(new ACMEAlreadyRevokedException("msg"), 400),
                Arguments.of(new ACMEBadCsrException("msg"), 400),
                Arguments.of(new ACMEBadNonceException("msg"), 400),
                Arguments.of(new ACMEBadPublicKeyException("msg"), 400),
                Arguments.of(new ACMEBadRevocationReasonException("msg"), 400),
                Arguments.of(new ACMEBadSignatureAlgorithmException("msg"), 400),
                Arguments.of(new ACMEConnectionErrorException("msg"), 400),
                Arguments.of(new ACMEInvalidContactException("msg"), 403),
                Arguments.of(new ACMEMalformedException("msg"), 400),
                Arguments.of(new ACMERateLimitedException("msg"), 429),
                Arguments.of(new ACMERejectedIdentifierException("msg"), 400),
                Arguments.of(new ACMEUnauthorizedException("msg"), 403),
                Arguments.of(new ACMEServerInternalException("msg"), 500),
                Arguments.of(new ACMEUserActionRequiredException("msg"), 403)
        );
    }

    static Stream<Arguments> errorTypeProvider() {
        return Stream.of(
                Arguments.of(new ACMEUserActionRequiredException("msg"), "urn:ietf:params:acme:error:userActionRequired"),
                Arguments.of(new ACMEResourceNotFoundException("msg"), "urn:ietf:params:acme:error:notFound")
        );
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("exceptionProvider")
    @DisplayName("getHttpStatusCode returns expected status")
    void testHttpStatus(ACMEException exception, int expectedStatus) {
        assertEquals(expectedStatus, exception.getHttpStatusCode());
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("errorTypeProvider")
    @DisplayName("getErrorResponse returns expected type")
    void testErrorType(ACMEException exception, String expectedType) {
        assertEquals(expectedType, exception.getErrorResponse().getType());
    }
}
