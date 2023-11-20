package de.morihofi.acmeserver.config.helper;

import com.google.gson.*;
import de.morihofi.acmeserver.config.certificateAlgorithms.AlgorithmParams;
import de.morihofi.acmeserver.config.certificateAlgorithms.EcdsaAlgorithmParams;
import de.morihofi.acmeserver.config.certificateAlgorithms.RSAAlgorithmParams;

import java.lang.reflect.Type;

public class AlgorithmParamsDeserializer implements JsonDeserializer<AlgorithmParams> {

    @Override
    public AlgorithmParams deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String type = jsonObject.get("type").getAsString();

        return switch (type.toLowerCase()) {
            case "ecdsa" -> context.deserialize(json, EcdsaAlgorithmParams.class);
            case "rsa" -> context.deserialize(json, RSAAlgorithmParams.class);
            default -> throw new JsonParseException("Unknown element type: " + type);
        };
    }
}
