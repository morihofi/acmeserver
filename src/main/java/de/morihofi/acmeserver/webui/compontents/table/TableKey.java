/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
