package de.morihofi.acmeserver.config.helper;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import de.morihofi.acmeserver.config.DatabaseConfig;
import de.morihofi.acmeserver.config.databaseConfig.JDBCUrlDatabaseConfig;
import de.morihofi.acmeserver.config.databaseConfig.OldDatabaseConfig;

import java.lang.reflect.Type;

public class DatabaseConfigDeserializer implements JsonDeserializer<DatabaseConfig> {
    @Override
    public DatabaseConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        if (jsonObject.has("engine") && jsonObject.has("name")) {
            return context.deserialize(json, OldDatabaseConfig.class);
        }

        if (jsonObject.has("jdbcUrl")) {
            return context.deserialize(json, JDBCUrlDatabaseConfig.class);
        }

        throw new JsonParseException("Unknown database configuration object.");
    }
}
