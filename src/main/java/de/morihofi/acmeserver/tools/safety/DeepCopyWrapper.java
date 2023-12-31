package de.morihofi.acmeserver.tools.safety;

import java.io.*;

/**
 * A generic class for creating deep copies of serializable objects.
 *
 * <p>
 * The `DeepCopyWrapper` class allows you to create a deep copy of a serializable object.
 * Deep copying is useful when you want to duplicate an object and its entire object graph.
 * This class performs deep copying by serializing and deserializing the input object,
 * effectively creating a new independent copy.
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
