package de.morihofi.acmeserver.types.plugin;

/**
 * Supported data types for plugin configuration properties.
 */
public enum PluginPropertyType {
    /** Boolean value (true/false). */
    BOOLEAN,
    /** Numeric value (integer, double, etc.). */
    NUMBER,
    /** UTF-8 string value. */
    STRING,
    /** Arbitrary binary data stored as Base64. */
    BYTES
}
