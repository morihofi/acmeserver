package de.morihofi.acmeserver.cluster;

import com.google.gson.Gson;
import io.grpc.MethodDescriptor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class JsonMarshaller<T> implements MethodDescriptor.Marshaller<T> {
    private final Class<T> type;
    private final Gson gson = new Gson();

    public JsonMarshaller(Class<T> type) {
        this.type = type;
    }

    @Override
    public InputStream stream(T value) {
        return new ByteArrayInputStream(gson.toJson(value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Override
    public T parse(InputStream stream) {
        try {
            return gson.fromJson(new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8), type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
