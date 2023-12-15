package de.morihofi.acmeserver.config.helper;

import com.google.gson.*;
import de.morihofi.acmeserver.config.keyStoreHelpers.KeyStoreParams;
import de.morihofi.acmeserver.config.keyStoreHelpers.PKCS11KeyStoreParams;
import de.morihofi.acmeserver.config.keyStoreHelpers.PKCS12KeyStoreParams;

import java.lang.reflect.Type;

public class KeyStoreParamsDeserializer implements JsonDeserializer<KeyStoreParams> {
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
