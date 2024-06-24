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

package de.morihofi.acmeserver.tools.safety;

import java.util.ArrayList;
import java.util.List;

/**
 * A utility class for performing type-safe casting on Raw Types.
 *
 * <p>
 * The `TypeSafetyHelper` class provides a method for safely casting raw elements in collections to a specified target class type. This
 * helps ensure type safety when working with collections of heterogeneous elements.
 * </p>
 */
public class TypeSafetyHelper {

    /**
     * Safely casts elements in a list to a specified target class type.
     *
     * <p>
     * This method iterates through the given list and attempts to cast each element to the specified target class. If an element cannot be
     * cast to the target class, an IllegalArgumentException is thrown.
     * </p>
     *
     * @param listToCast  The list containing elements to be cast.
     * @param targetClass The class type to cast the elements to.
     * @param <T>         The type of elements in the target list.
     * @return A list containing elements of the specified target class type.
     * @throws IllegalArgumentException If an element in the list cannot be cast to the target class type.
     */
    @SuppressWarnings({"rawtypes"})
    public static <T> List<T> safeCastToClassOfType(List listToCast, Class<T> targetClass) {
        List<T> targetList = new ArrayList<>();

        for (Object obj : listToCast) {
            if (targetClass.isInstance(obj)) {
                targetList.add(targetClass.cast(obj));
            } else {
                throw new IllegalArgumentException(
                        "Object type is not of the target class. Expected " + targetClass.getName() + " but got " + obj.getClass());
            }
        }

        return targetList;
    }

    /**
     * Private constructor to prevent instantiation of the utility class.
     */
    private TypeSafetyHelper() {}
}
