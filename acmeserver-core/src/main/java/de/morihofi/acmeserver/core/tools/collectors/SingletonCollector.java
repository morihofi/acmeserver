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

package de.morihofi.acmeserver.core.tools.collectors;


import lombok.extern.slf4j.Slf4j;

import java.lang.invoke.MethodHandles;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Utility class for providing a custom collector that ensures a collection contains exactly one element.
 *
 * <p>
 * This class provides a method to create a custom collector that collects elements into a singleton. If the collection
 * contains more or fewer than one element, an {@link IllegalStateException} is thrown.
 * </p>
 */
public class SingletonCollector {

    /**
     * Creates a collector that ensures the collection contains exactly one element.
     *
     * <p>
     * This method returns a collector that collects elements into a list and then ensures the list contains exactly
     * one element. If the list does not contain exactly one element, an {@link IllegalStateException} is thrown.
     * </p>
     *
     * @param <T> the type of elements being collected
     * @return a collector that ensures the collection contains exactly one element
     */
    public static <T> Collector<T, ?, T> toSingleton() {
        return Collectors.collectingAndThen(
                Collectors.toList(),
                list -> {
                    if (list.size() != 1) {
                        throw new IllegalStateException("Collection does not contain exactly one element");
                    }
                    return list.get(0);
                }
        );
    }
}
