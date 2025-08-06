/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.lambda;

@FunctionalInterface
public interface BiFunctionWithException<A, B, R> {
    R apply(A a, B b) throws Exception;

    default <V> BiFunctionWithException<A, B, V> andThen(java.util.function.Function<? super R, ? extends V> after) {
        return (a, b) -> after.apply(apply(a, b));
    }
}

