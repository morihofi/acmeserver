package de.morihofi.acmeserver.webui.compontents.table;

/**
 * Represents a key for use in tables within a web user interface, incorporating both a textual value and optional icon classes for visual
 * enhancement. The textual value often serves as a label or identifier, while the icon classes allow for the inclusion of FontAwesome
 * icons, adding a visual element to the table entry. This dual nature supports a more informative and user-friendly presentation of data.
 */
public class TableKey {
    /**
     * The textual value of the table key, serving as an identifier or label.
     */
    private final String value;

    /**
     * The CSS classes for a FontAwesome icon, enhancing the key's visual presentation. This field is optional and may be null if no icon is
     * to be displayed.
     */
    private final String iconClasses;

    /**
     * Constructs a new TableKey with both a value and icon classes, enabling the display of a FontAwesome icon alongside the textual
     * value.
     *
     * @param value       The textual value of the key.
     * @param iconClasses The CSS classes for the FontAwesome icon to be displayed with the key. This parameter is optional and can be null
     *                    or empty if no icon is desired.
     */
    public TableKey(String value, String iconClasses) {
        this.value = value;
        this.iconClasses = iconClasses;
    }

    /**
     * Constructs a new TableKey without any icon classes, resulting in a key that only displays its textual value.
     *
     * @param value The textual value of the key.
     */
    public TableKey(String value) {
        this.value = value;
        this.iconClasses = null;
    }

    /**
     * Retrieves the textual value of this table key.
     *
     * @return The textual value of the key.
     */
    public String getValue() {
        return value;
    }

    /**
     * Retrieves the CSS classes for the FontAwesome icon associated with this table key. May return null if no icon classes were specified
     * during construction.
     *
     * @return The CSS classes for the FontAwesome icon, or null if no icon is to be displayed.
     */
    public String getIconClasses() {
        return iconClasses;
    }
}
