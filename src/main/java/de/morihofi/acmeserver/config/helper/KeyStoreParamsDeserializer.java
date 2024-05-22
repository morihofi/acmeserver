package de.morihofi.acmeserver.config.helper;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import de.morihofi.acmeserver.config.keyStoreHelpers.KeyStoreParams;
import de.morihofi.acmeserver.config.keyStoreHelpers.PKCS11KeyStoreParams;
import de.morihofi.acmeserver.config.keyStoreHelpers.PKCS12KeyStoreParams;

import java.lang.reflect.Type;

/**
 * Deserializer for {@link KeyStoreParams} objects. This class implements the {@link JsonDeserializer} interface to provide custom
 * deserialization for different types of KeyStore parameters, such as PKCS11 and PKCS12.
 */
public class KeyStoreParamsDeserializer implements JsonDeserializer<KeyStoreParams> {

    /**
     * Deserializes a JSON element into an appropriate {@link KeyStoreParams} subclass. The specific subclass is determined based on the
     * 'type' field in the JSON data.
     *
     * @param json    The JSON data being deserialized.
     * @param typeOfT The type of the Object to deserialize to.
     * @param context The deserialization context.
     * @return An instance of either {@link PKCS11KeyStoreParams} or {@link PKCS12KeyStoreParams} depending on the 'type' field.
     * @throws JsonParseException if the 'type' field is unknown or missing.
     */
    @Override
    public KeyStoreParams deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String type = jsonObject.get("type").getAsString();

        return switch (type.toLowerCase()) {
            case "pkcs11" -> context.deserialize(json, PKCS11KeyStoreParams.class);
            case "pkcs12" -> context.deserialize(json, PKCS12KeyStoreParams.class);
            default -> throw new JsonParseException("Unknown keyStore type: " + type);
        };
    }
}
