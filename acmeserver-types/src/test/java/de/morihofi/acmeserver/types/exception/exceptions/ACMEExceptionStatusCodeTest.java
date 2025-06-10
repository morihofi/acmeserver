package de.morihofi.acmeserver.types.exception.exceptions;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.DisplayName;
import de.morihofi.acmeserver.types.exception.ACMEException;
import de.morihofi.acmeserver.types.exception.exceptions.ACMEUserActionRequiredException;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for verifying HTTP status codes of ACME exceptions.
 */
class ACMEExceptionStatusCodeTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("exceptionProvider")
    @DisplayName("getHttpStatusCode returns expected status")
    void testHttpStatus(ACMEException exception, int expectedStatus) {
        assertEquals(expectedStatus, exception.getHttpStatusCode());
    }

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

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("errorTypeProvider")
    @DisplayName("getErrorResponse returns expected type")
    void testErrorType(ACMEException exception, String expectedType) {
        assertEquals(expectedType, exception.getErrorResponse().getType());
    }

    static Stream<Arguments> errorTypeProvider() {
        return Stream.of(
                Arguments.of(new ACMEUserActionRequiredException("msg"), "urn:ietf:params:acme:error:userActionRequired"),
                Arguments.of(new ACMEResourceNotFoundException("msg"), "urn:ietf:params:acme:error:notFound")
        );
    }
}
