package de.morihofi.certgine.types.json;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Utility factory for creating {@link Gson} instances with
 * {@link java.time.Instant} support using
 * {@link DateTimeFormatter#ISO_INSTANT} in UTC.
 */
public final class GsonFactory {

    private GsonFactory() {
    }

    /**
     * Creates a new {@link Gson} instance configured for ISO‑Instant
     * serialization.
     *
     * @return configured {@link Gson}
     */
    public static Gson createGson() {
        return baseBuilder().create();
    }

    /**
     * Provides a base {@link GsonBuilder} with the ISO‑Instant adapter
     * pre‑registered. Additional adapters can be registered on the returned
     * builder before calling {@link GsonBuilder#create()}.
     *
     * @return a {@link GsonBuilder} with ISO‑Instant support
     */
    public static GsonBuilder baseBuilder() {
        return new GsonBuilder().registerTypeAdapter(Instant.class, new InstantAdapter());
    }

    private static final class InstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {

        private static final DateTimeFormatter FORMATTER =
                DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return JsonNull.INSTANCE;
            }
            return new JsonPrimitive(FORMATTER.format(src));
        }

        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return null;
            }
            return Instant.from(FORMATTER.parse(json.getAsString()));
        }
    }
}

