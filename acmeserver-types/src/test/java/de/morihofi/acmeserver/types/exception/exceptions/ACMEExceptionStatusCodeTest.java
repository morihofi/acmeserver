package de.morihofi.acmeserver.types.exception.exceptions;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.DisplayName;
import de.morihofi.acmeserver.types.exception.ACMEException;
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
                Arguments.of(new ACMEAlreadyRevokedException("msg"), 400),
                Arguments.of(new ACMEConnectionErrorException("msg"), 400),
                Arguments.of(new ACMEBadCsrException("msg"), 400),
                Arguments.of(new ACMEBadRevocationReasonException("msg"), 400),
                Arguments.of(new ACMEMalformedException("msg"), 400)
        );
    }
}
