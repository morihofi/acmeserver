package de.morihofi.acmeserver.config.helper;

import com.google.gson.*;
import de.morihofi.acmeserver.config.certificateAlgorithms.AlgorithmParams;
import de.morihofi.acmeserver.config.certificateAlgorithms.EcdsaAlgorithmParams;
import de.morihofi.acmeserver.config.certificateAlgorithms.RSAAlgorithmParams;

import java.lang.reflect.Type;

/**
 * Deserializer for {@link AlgorithmParams} objects.
 * This class implements the {@link JsonDeserializer} interface and provides custom deserialization
 * for subclasses of AlgorithmParams, specifically {@link EcdsaAlgorithmParams} and {@link RSAAlgorithmParams}.
 */
public class AlgorithmParamsDeserializer implements JsonDeserializer<AlgorithmParams> {

    /**
     * Deserializes a JSON element into an appropriate {@link AlgorithmParams} subclass.
     * The specific subclass is determined based on the 'type' field in the JSON data.
     *
     * @param json The JSON data being deserialized.
     * @param typeOfT The type of the Object to deserialize to.
     * @param context The deserialization context.
     * @return An instance of {@link EcdsaAlgorithmParams} or {@link RSAAlgorithmParams} depending on the 'type' field.
     * @throws JsonParseException if the 'type' field is unknown or missing.
     */
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
