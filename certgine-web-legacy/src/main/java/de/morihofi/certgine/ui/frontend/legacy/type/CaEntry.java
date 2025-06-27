package de.morihofi.certgine.ui.frontend.legacy.type;

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

    private String commonName;
    private String organisation;
    private String organisationalUnit;
    private String countryCode;
    private String sha1Fingerprint;
    private String sha256Fingerprint;
    private String algorithmDetail;
}
