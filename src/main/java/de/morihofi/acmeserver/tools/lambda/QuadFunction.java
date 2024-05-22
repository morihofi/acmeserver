package de.morihofi.acmeserver.tools.lambda;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface QuadFunction<A, B, C, D, R> {

    /**
     * Applies this function to the given arguments.
     *
     * @param a the first function argument
     * @param b the second function argument
     * @param c the third function argument
     * @param d the fourth function argument
     * @return the function result
     * @throws Exception if the function cannot be applied to the given arguments
     */
    R apply(A a, B b, C c, D d) throws Exception;

    /**
     * Returns a composed function that first applies this function to its input, and then applies the {@code after} function to the
     * result.
     *
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default <V> QuadFunction<A, B, C, D, V> andThen(
            Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (A a, B b, C c, D d) -> after.apply(apply(a, b, c, d));
    }
}
