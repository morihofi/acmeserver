package de.morihofi.acmeserver.webui.compontents.table;

/**
 * Represents the value part of a table entry in a web user interface, encapsulating the actual data to be displayed and its type. The data
 * can be presented as plain text or as a hyperlink, depending on the specified {@link VALUE_TYPE}. This class allows for a flexible
 * representation of table values, facilitating the rendering of different types of content within table cells.
 */
public class TableValue {
    /**
     * The actual data or content to be displayed in the table cell.
     */
    private final String value;

    /**
     * The type of the value, indicating how it should be rendered (e.g., as plain text or a hyperlink).
     */
    private final VALUE_TYPE valueType;

    /**
     * Constructs a new TableValue with the specified content and value type. This constructor allows for the explicit specification of the
     * value's type, facilitating the appropriate rendering of the value in the UI.
     *
     * @param value     The content or data to be displayed in the table cell.
     * @param valueType The type of the value, determining how it is rendered (e.g., as plain text or a hyperlink).
     */
    public TableValue(String value, VALUE_TYPE valueType) {
        this.value = value;
        this.valueType = valueType;
    }

    /**
     * Constructs a new TableValue with the specified content, defaulting to a value type of {@code NONE}. This constructor is convenient
     * when the value is plain text and does not require special rendering as a hyperlink.
     *
     * @param value The content or data to be displayed in the table cell, assumed to be plain text.
     */
    public TableValue(String value) {
        this.value = value;
        this.valueType = VALUE_TYPE.NONE;
    }

    /**
     * Retrieves the content or data of this table value.
     *
     * @return The content or data to be displayed in the table cell.
     */
    public String getValue() {
        return value;
    }

    /**
     * Retrieves the type of this table value, indicating how it should be rendered in the UI.
     *
     * @return The type of the value, either {@code NONE} for plain text or {@code LINK} for hyperlinks.
     */
    public VALUE_TYPE getValueType() {
        return valueType;
    }

    /**
     * Enumerates the possible types of values that can be represented, allowing for differentiation between plain text values and
     * hyperlinks.
     */
    public enum VALUE_TYPE {
        NONE, // Indicates that the value is plain text and should not be rendered as a link.
        LINK  // Indicates that the value is a URL and should be rendered as a hyperlink.
    }
}
