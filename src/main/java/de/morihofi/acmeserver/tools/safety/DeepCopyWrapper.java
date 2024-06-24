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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * A generic class for creating deep copies of serializable objects.
 *
 * <p>
 * The `DeepCopyWrapper` class allows you to create a deep copy of a serializable object. Deep copying is useful when you want to duplicate
 * an object and its entire object graph. This class performs deep copying by serializing and deserializing the input object, effectively
 * creating a new independent copy.
 * </p>
 *
 * @param <T> The type of the object to be deep-copied, which must implement the `Serializable` interface.
 */
public class DeepCopyWrapper<T extends Serializable> implements Serializable {
    private final T object;

    /**
     * Constructs a `DeepCopyWrapper` with a deep copy of the provided object.
     *
     * @param object The object to be deep-copied.
     */
    public DeepCopyWrapper(T object) {
        this.object = deepCopy(object);
    }

    /**
     * ✨ Does the fancy deep copy stuff ✨
     *
     * @param object object to deep copy
     * @return copy of the object
     */
    @SuppressWarnings("unchecked")
    private T deepCopy(T object) {
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(byteOut)) {
                out.writeObject(object);
            }

            ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
            try (ObjectInputStream in = new ObjectInputStream(byteIn)) {
                return (T) in.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Deep copy failed", e);
        }
    }

    /**
     * Gets the deep-copied object.
     *
     * @return The deep-copied object.
     */
    public T getObject() {
        return object;
    }
}
