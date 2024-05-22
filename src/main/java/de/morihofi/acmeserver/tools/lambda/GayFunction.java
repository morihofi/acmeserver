package de.morihofi.acmeserver.tools.lambda;

/**
 * A functional interface similar to {@code Function}, but with a playful twist. It represents a function that accepts one argument and
 * produces a result of the same type. This interface is intended for educational or fun purposes and showcases the concept of identity
 * function in a generic way.
 *
 * <p>This is an easter egg</p>
 *
 * @param <T> the type of the input and output of the function
 */
@FunctionalInterface
public interface GayFunction<T> {
    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result, which is the same as the input argument
     */
    T apply(T t);
}
