package de.morihofi.acmeserver.ui.frontend.legacy.type;

import lombok.Data;

@Data
public class CaEntry {
    private String name;
    private String description;
    private String pemPath;
    private String derPath;
    private String cabPath;
    private String id;
    private boolean primary;
    private boolean ecdsa;
}
