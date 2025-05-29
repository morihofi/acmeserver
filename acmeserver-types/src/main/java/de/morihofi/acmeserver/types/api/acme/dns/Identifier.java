package de.morihofi.acmeserver.types.api.acme.dns;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Locale;

/**
 * ACME Identifier used in Requests from ACME Clients
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class Identifier {

    /**
     * Type of the DNS identifier, mostly <code>dns</code>. Can also be <code>ip</code>
     */
    private String type;

    /**
     * Value of the identifier, so it is the DNS Name
     */
    private String value;

    public Identifier(IDENTIFIER_TYPE type, String value) {
        this.type = type.name().toLowerCase(Locale.ROOT);
        this.value = value;
    }

    public Identifier(String type, String value) {
        this(IDENTIFIER_TYPE.getTypeByName(type), value);
    }

    public IDENTIFIER_TYPE getTypeAsEnumConstant() {
        return IDENTIFIER_TYPE.getTypeByName(type.toLowerCase(Locale.ROOT));
    }

    public void setType(String type) {
        this.type = IDENTIFIER_TYPE.getTypeByName(type).name().toLowerCase(Locale.ROOT);
    }

    public enum IDENTIFIER_TYPE {
        DNS, IP;

        public static IDENTIFIER_TYPE getTypeByName(String name) {
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "dns" -> DNS;
                case "ip" -> IP;
                default -> throw new IllegalArgumentException("Unknown or unsupported identifier type " + name);
            };
        }
    }
}
