package de.morihofi.acmeserver.webui.compontents.table;

public class TableKey {
    private final String value;
    private final String iconClasses;

    public TableKey(String value, String iconClasses) {
        this.value = value;
        this.iconClasses = iconClasses;
    }

    public TableKey(String value) {
        this.value = value;
        this.iconClasses = null;
    }

    public String getValue() {
        return value;
    }

    public String getIconClasses() {
        return iconClasses;
    }
}
