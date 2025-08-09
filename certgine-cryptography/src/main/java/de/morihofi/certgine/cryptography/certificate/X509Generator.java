/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.cryptography.certificate;

import de.morihofi.certgine.types.database.entities.authority.CertificateConfig;
import de.morihofi.certgine.types.dns.DnsIdentifier;
import de.morihofi.certgine.types.intf.IServerInstance;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.operator.OperatorCreationException;

/**
 * Single entry point for creating every certificate variant (ROOT‑CA, INTERMEDIATE‑CA, SERVER)
 * with a single {@link Request} generated via Lombok's builder.
 */
public class X509Generator {

    private static final Map<Type, ICertificateGenerator> GENERATORS = Map.of(
            Type.ROOT_CA, new RootCaGenerator(),
            Type.INTERMEDIATE_CA, new IntermediateCaGenerator(),
            Type.SERVER, new ServerCertGenerator(),
            Type.TIMESTAMPING, new TimestampAuthorityGenerator(),
            Type.CODE_SIGNING, new CodeSigningGenerator());

    /**
     * Creates the requested X509 certificate.
     */
    public static X509Certificate generate(@NonNull Request req)
            throws CertificateException, OperatorCreationException, CertIOException {
        ICertificateGenerator generator = GENERATORS.get(req.getType());
        if (generator == null) {
            throw new IllegalArgumentException("Unsupported certificate type: " + req.getType());
        }
        return generator.generate(req);
    }

    /**
     * Describes the certificate flavour that should be produced.
     */
    public enum Type {
        /** Root certificate authority. */
        ROOT_CA,
        /** Intermediate certificate authority. */
        INTERMEDIATE_CA,
        /** Server certificate. */
        SERVER,
        /** Timestamp authority certificate. */
        TIMESTAMPING,
        /** Code signing certificate. */
        CODE_SIGNING
    }

    /**
     * Immutable request object that gathers **all** inputs required by the generation
     * paths. Only the relevant subset must be filled for a concrete {@link Type}. Validation
     * happens at runtime to keep the public API minimal.
     */
    @Getter
    @Builder
    public static class Request {
        /** Certificate flavour that shall be generated. */
        @NonNull
        private final Type type;

        /* ——— Common parameters ——— */
        private final CertificateConfig certificateConfig;
        private final IServerInstance serverInstance;

        /* ——— Key material ——— */
        private final KeyPair ownKeyPair;                 // generated key‑pair for the certificate itself (ROOT + INTERMEDIATE)
        private final KeyPair issuerKeyPair;              // key‑pair of the issuer/parent (INTERMEDIATE + SERVER)
        private final X509Certificate issuerCertificate;  // cert of the issuer/parent (INTERMEDIATE + SERVER)

        /* ——— ROOT/INTERMEDIATE specific ——— */
        private final String crlDistributionUrl;          // INTERMEDIATE (optional for ROOT)
        private final String ocspServiceEndpoint;         // INTERMEDIATE (optional for ROOT)

        /* ——— Server‑certificate specifics ——— */
        private final byte[] serverPublicKeyBytes;
        @Singular
        private final List<DnsIdentifier> identifiers;
        private final Date startDate;
        private final Date endDate;
    }
}

