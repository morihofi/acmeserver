package de.morihofi.acmeserver.types.database.entities;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.regex.Pattern;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CertificateMetadata implements Serializable {
    private String commonName;
    private String organisation;
    private String organisationalUnit;
    private String countryCode;
    private String email;

    /**
     * Custom builder to validate inputs before object creation.
     */
    public static class CertificateMetadataBuilder {
        /**
         * Builds the {@link CertificateMetadata} instance and validates all
         * fields for correct format and length.
         *
         * @return validated {@link CertificateMetadata}
         * @throws IllegalArgumentException when validation fails
         */
        public CertificateMetadata build() {
            CertificateMetadata meta = new CertificateMetadata(commonName, organisation,
                    organisationalUnit, countryCode, email);
            validate(meta);
            return meta;
        }

        private static void validate(CertificateMetadata m) {
            if (m.commonName == null || m.commonName.isEmpty()) {
                throw new IllegalArgumentException("commonName must not be empty");
            }
            if (m.commonName.length() > 64) {
                throw new IllegalArgumentException("commonName exceeds 64 characters");
            }
            if (m.organisation != null && m.organisation.length() > 64) {
                throw new IllegalArgumentException("organisation exceeds 64 characters");
            }
            if (m.organisationalUnit != null && m.organisationalUnit.length() > 64) {
                throw new IllegalArgumentException("organisationalUnit exceeds 64 characters");
            }
            if (m.countryCode != null && !m.countryCode.isEmpty()
                    && !m.countryCode.matches("[A-Z]{2}")) {
                throw new IllegalArgumentException("countryCode must be two uppercase letters");
            }
            if (m.email != null && !m.email.isEmpty()) {
                String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
                if (!Pattern.compile(emailRegex).matcher(m.email).matches()) {
                    throw new IllegalArgumentException("email has invalid format");
                }
            }
        }
    }
}

