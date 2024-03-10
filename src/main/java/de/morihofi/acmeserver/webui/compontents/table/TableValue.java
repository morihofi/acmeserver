package de.morihofi.acmeserver.webui.compontents.table;

public class TableValue {
    private final String value;
    private final VALUE_TYPE valueType;

    public enum VALUE_TYPE {
        NONE, LINK

    }

    public TableValue(String value, VALUE_TYPE valueType) {
        this.value = value;
        this.valueType = valueType;
    }

    public TableValue(String value){
        this.value = value;
        this.valueType = VALUE_TYPE.NONE;
    }

    public String getValue() {
        return value;
    }

    public VALUE_TYPE getValueType() {
        return valueType;
    }
}
