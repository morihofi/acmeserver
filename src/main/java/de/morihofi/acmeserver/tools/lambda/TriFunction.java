package de.morihofi.acmeserver.tools.lambda;

import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public /**
 * Same as {@See BiFunction}, but with 3 Arguments
 */
interface TriFunction<A,B,C,R> {

    R apply(A a, B b, C c) throws Exception;

    default <V> TriFunction<A, B, C, V> andThen(
            Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (A a, B b, C c) -> after.apply(apply(a, b, c));
    }
}
