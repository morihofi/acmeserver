/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de> 🏳️‍🌈
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
