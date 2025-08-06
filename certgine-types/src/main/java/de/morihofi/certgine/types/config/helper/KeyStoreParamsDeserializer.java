/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.config.helper;

import com.google.gson.*;
import de.morihofi.certgine.types.config.keyStoreHelpers.KeyStoreParams;
import de.morihofi.certgine.types.config.keyStoreHelpers.PKCS11KeyStoreParams;
import de.morihofi.certgine.types.config.keyStoreHelpers.PKCS12KeyStoreParams;
import lombok.NonNull;

import java.lang.reflect.Type;
import java.util.Arrays;

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
    @NonNull
    public KeyStoreParams deserialize(@NonNull JsonElement json,
                                      @NonNull Type typeOfT,
                                      @NonNull JsonDeserializationContext context) throws JsonParseException {

        JsonObject jsonObject = json.getAsJsonObject();

        // extract and convert the password from String or char[] to char[]
        char[] password = null;
        if (jsonObject.has("password") && !jsonObject.get("password").isJsonNull()) {
            JsonElement pwd = jsonObject.get("password");

            if (pwd.isJsonArray()) {                       // still accept the exotic array form
                JsonArray arr = pwd.getAsJsonArray();
                password = new char[arr.size()];
                for (int i = 0; i < arr.size(); i++) {
                    password[i] = arr.get(i).getAsString().charAt(0);
                }
            } else {                                       // normal "password": "s3cr3t"
                password = pwd.getAsString().toCharArray();
            }
        }

        // Remove the field so that Gson’s default adapter will not
        // attempt to handle char[] on its own and explode again.
        jsonObject.remove("password");

        // 2. choose the concrete subclass
        String type = jsonObject.get("type").getAsString();
        KeyStoreParams params = switch (type.toLowerCase()) {
            case "pkcs11" -> context.deserialize(jsonObject, PKCS11KeyStoreParams.class);
            case "pkcs12" -> context.deserialize(jsonObject, PKCS12KeyStoreParams.class);
            default -> throw new JsonParseException("Unknown keyStore type: " + type);
        };

        // 3. inject the password we converted
        params.setPassword(Arrays.copyOf(password, password.length));

        // overwrite the password field in the params object for security reasons
        Arrays.fill(password, '\0');
        password = null;

        return params;
    }
}
