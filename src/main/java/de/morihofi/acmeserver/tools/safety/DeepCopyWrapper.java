package de.morihofi.acmeserver.tools.safety;

import java.io.*;

public class DeepCopyWrapper<T extends Serializable> implements Serializable {
    private final T object;

    public DeepCopyWrapper(T object) {
        this.object = deepCopy(object);
    }

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

    public T getObject() {
        return object;
    }
}
