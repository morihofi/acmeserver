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

package de.morihofi.certgine.utils.collectors;


import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

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
    public static <T> Collector<T, List<T>, T> toSingleton() {
        return new Collector<>() {

            @Override
            public Supplier<List<T>> supplier() {
                return ArrayList::new;
            }

            @Override
            public BiConsumer<List<T>, T> accumulator() {
                return (list, item) -> {
                    if (!list.isEmpty()) {
                        throw new IllegalStateException("Collection does not contain exactly one element");
                    }
                    list.add(item);
                };
            }

            @Override
            public BinaryOperator<List<T>> combiner() {
                return (left, right) -> {
                    if (!left.isEmpty() && !right.isEmpty()) {
                        throw new IllegalStateException("Collection does not contain exactly one element");
                    }
                    return left.isEmpty() ? right : left;
                };
            }

            @Override
            public Function<List<T>, T> finisher() {
                return list -> {
                    if (list.size() != 1) {
                        throw new IllegalStateException("Collection does not contain exactly one element");
                    }
                    return list.getFirst();
                };
            }

            @Override
            public Set<Characteristics> characteristics() {
                return EnumSet.noneOf(Characteristics.class);
            }
        };
    }

}
