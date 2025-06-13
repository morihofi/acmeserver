package de.morihofi.acmeserver.types.plugin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Represents a single configuration property for a plugin.
 * Values are stored as strings to simplify serialization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginProperty {
    private String key;
    private PluginPropertyType type;
    private String value;

    /** Creates a boolean property. */
    public static PluginProperty ofBoolean(String key, boolean v) {
        return PluginProperty.builder()
                .key(key)
                .type(PluginPropertyType.BOOLEAN)
                .value(Boolean.toString(v))
                .build();
    }

    /** Creates a numeric property using {@code Double.toString}. */
    public static PluginProperty ofNumber(String key, Number v) {
        return PluginProperty.builder()
                .key(key)
                .type(PluginPropertyType.NUMBER)
                .value(Double.toString(v.doubleValue()))
                .build();
    }

    /** Creates a string property. */
    public static PluginProperty ofString(String key, String v) {
        return PluginProperty.builder()
                .key(key)
                .type(PluginPropertyType.STRING)
                .value(v)
                .build();
    }

    /** Creates a binary property encoded as Base64. */
    public static PluginProperty ofBytes(String key, byte[] v) {
        return PluginProperty.builder()
                .key(key)
                .type(PluginPropertyType.BYTES)
                .value(Base64.getEncoder().encodeToString(v))
                .build();
    }

    /** Returns the property value interpreted as a boolean. */
    public boolean asBoolean() {
        return Boolean.parseBoolean(value);
    }

    /** Returns the property value interpreted as a double. */
    public double asNumber() {
        return Double.parseDouble(value);
    }

    /** Returns the property value interpreted as a string. */
    public String asString() {
        return value;
    }

    /** Returns the property value decoded from Base64. */
    public byte[] asBytes() {
        return Base64.getDecoder().decode(value.getBytes(StandardCharsets.UTF_8));
    }
}
